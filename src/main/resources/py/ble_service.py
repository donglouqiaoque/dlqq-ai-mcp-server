#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BLE Bridge Service
通过 Socket 提供 BLE 功能，供 Java 调用
使用 bleak 库实现跨平台 BLE 操作
"""

import asyncio
import json
import sys
import threading
import socket
from typing import Optional, Dict, Any, List, Callable
from bleak import BleakClient, BleakScanner, BleakError
from bleak.backends.characteristic import BleakGATTCharacteristic


class BleBridgeService:
    """BLE桥接服务"""

    def __init__(self, port: int = 8765):
        self.port = port
        self.server_socket: Optional[socket.socket] = None
        self.running = False
        self.client: Optional[BleakClient] = None
        self.connected_device: Optional[str] = None
        self.notification_callbacks: Dict[str, Callable] = {}
        self.is_scanning: bool = False
        # 通知推送配置: {char_uuid: {"java_port": int, "service_uuid": str}}
        self.notification_configs: Dict[str, Dict] = {}

    def start(self):
        """启动服务"""
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind(('127.0.0.1', self.port))
        self.server_socket.listen(1)
        self.running = True

        print(f"[BLE Bridge] 服务启动，监听端口: {self.port}", flush=True)

        while self.running:
            try:
                conn, addr = self.server_socket.accept()
                print(f"[BLE Bridge] 客户端连接: {addr}", flush=True)
                self.handle_client(conn)
            except Exception as e:
                print(f"[BLE Bridge] 接受连接失败: {e}", flush=True)

    def handle_client(self, conn: socket.socket):
        """处理客户端连接"""
        try:
            while self.running:
                # 接收命令 - 使用循环读取直到收到换行符
                buffer = b""
                while self.running:
                    chunk = conn.recv(4096)
                    if not chunk:
                        return
                    buffer += chunk
                    if b'\n' in buffer:
                        break

                data = buffer.decode('utf-8').strip()
                if not data:
                    break

                # 解析JSON命令
                try:
                    command = json.loads(data)
                    result = self.execute_command(command)
                except json.JSONDecodeError as e:
                    result = {"success": False, "error": f"Invalid JSON: {str(e)}"}

                # 发送结果 - 分块发送大响应
                response = json.dumps(result, ensure_ascii=False) + "\n"
                response_bytes = response.encode('utf-8')

                # 分块发送
                total_sent = 0
                while total_sent < len(response_bytes):
                    sent = conn.send(response_bytes[total_sent:total_sent+4096])
                    if sent == 0:
                        raise RuntimeError("Socket connection broken")
                    total_sent += sent

        except Exception as e:
            print(f"[BLE Bridge] 客户端处理异常: {e}", flush=True)
        finally:
            conn.close()
            print("[BLE Bridge] 客户端断开", flush=True)

    def execute_command(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """执行命令"""
        cmd_type = command.get("type")

        handlers = {
            "scan": self.handle_scan,
            "connect": self.handle_connect,
            "disconnect": self.handle_disconnect,
            "discover_services": self.handle_discover_services,
            "read": self.handle_read,
            "write": self.handle_write,
            "subscribe": self.handle_subscribe,
            "unsubscribe": self.handle_unsubscribe,
            "is_connected": self.handle_is_connected,
            "is_scanning": self.handle_is_scanning,
            "ping": self.handle_ping,
        }

        handler = handlers.get(cmd_type)
        if handler:
            return handler(command)
        return {"success": False, "error": f"Unknown command: {cmd_type}"}

    def run_async(self, coro):
        """运行异步协程"""
        try:
            loop = asyncio.get_event_loop()
        except RuntimeError:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
        return loop.run_until_complete(coro)

    def handle_scan(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """扫描BLE设备"""
        timeout = command.get("timeout", 10.0)

        async def scan():
            """使用 BleakScanner.discover() 静态方法进行扫描"""
            devices = []
            discovered = await BleakScanner.discover(timeout=timeout)
            for device in discovered:
                # bleak 0.20+ 版本中 BLEDevice 的 rssi 可能为 None
                rssi = getattr(device, 'rssi', None) or -100
                devices.append({
                    "address": device.address,
                    "name": device.name or "Unknown",
                    "rssi": rssi,
                })
            return devices

        try:
            self.is_scanning = True
            devices = self.run_async(scan())
            return {"success": True, "devices": devices}
        except Exception as e:
            import traceback
            return {"success": False, "error": str(e), "traceback": traceback.format_exc()}
        finally:
            self.is_scanning = False

    def handle_connect(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """连接设备"""
        address = command.get("address")
        if not address:
            return {"success": False, "error": "Address required"}

        if self.client and self.client.is_connected:
            return {"success": False, "error": "Already connected"}

        async def connect():
            self.client = BleakClient(address)
            await self.client.connect()
            return self.client.is_connected

        try:
            connected = self.run_async(connect())
            if connected:
                self.connected_device = address
                return {"success": True, "address": address}
            return {"success": False, "error": "Connection failed"}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def handle_disconnect(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """断开连接"""
        if not self.client or not self.client.is_connected:
            return {"success": True, "message": "Not connected"}

        async def disconnect():
            await self.client.disconnect()
            return True

        try:
            self.run_async(disconnect())
            self.client = None
            self.connected_device = None
            self.notification_callbacks.clear()
            self.notification_configs.clear()  # 清理通知配置
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def handle_discover_services(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """发现服务"""
        if not self.client or not self.client.is_connected:
            return {"success": False, "error": "Not connected"}

        async def discover():
            services = []
            for service in self.client.services:
                chars = []
                for char in service.characteristics:
                    props = []
                    if "read" in char.properties:
                        props.append("read")
                    if "write" in char.properties or "write-without-response" in char.properties:
                        props.append("write")
                    if "notify" in char.properties or "indicate" in char.properties:
                        props.append("notify")
                    chars.append({
                        "uuid": char.uuid,
                        "name": char.description or "Unknown",
                        "properties": props,
                    })
                services.append({
                    "uuid": service.uuid,
                    "name": service.description or "Unknown",
                    "characteristics": chars,
                })
            return services

        try:
            services = self.run_async(discover())
            return {"success": True, "services": services}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def handle_read(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """读取特征值"""
        if not self.client or not self.client.is_connected:
            return {"success": False, "error": "Not connected"}

        char_uuid = command.get("char_uuid")
        if not char_uuid:
            return {"success": False, "error": "Characteristic UUID required"}

        async def read():
            data = await self.client.read_gatt_char(char_uuid)
            return data

        try:
            data = self.run_async(read())
            return {
                "success": True,
                "data": list(data),  # 转为列表便于JSON序列化
                "hex": data.hex(),
                "text": data.decode('utf-8', errors='ignore'),
            }
        except Exception as e:
            return {"success": False, "error": str(e)}

    def handle_write(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """写入特征值"""
        if not self.client or not self.client.is_connected:
            return {"success": False, "error": "Not connected"}

        char_uuid = command.get("char_uuid")
        data = command.get("data")
        response = command.get("response", True)

        if not char_uuid or data is None:
            return {"success": False, "error": "Characteristic UUID and data required"}

        async def write():
            byte_data = bytes(data) if isinstance(data, list) else data.encode('utf-8')
            await self.client.write_gatt_char(char_uuid, byte_data, response=response)
            return True

        try:
            self.run_async(write())
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def handle_subscribe(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """订阅通知"""
        if not self.client or not self.client.is_connected:
            return {"success": False, "error": "Not connected"}

        char_uuid = command.get("char_uuid")
        service_uuid = command.get("service_uuid", "")
        notification_port = command.get("notification_port")
        if not char_uuid:
            return {"success": False, "error": "Characteristic UUID required"}

        # 保存通知配置，用于后续推送
        if notification_port:
            self.notification_configs[char_uuid] = {
                "java_port": notification_port,
                "service_uuid": service_uuid
            }

        async def subscribe():
            def callback(sender: BleakGATTCharacteristic, data: bytearray):
                # 打印到本地日志
                print(f"[Notification] {char_uuid}: {data.hex()}", flush=True)
                # 推送到Java通知端口
                self._push_notification(char_uuid, data)

            await self.client.start_notify(char_uuid, callback)
            return True

        try:
            self.run_async(subscribe())
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def _push_notification(self, char_uuid: str, data: bytearray):
        """推送通知到Java端"""
        import json
        config = self.notification_configs.get(char_uuid)
        if not config:
            return

        java_port = config.get("java_port")
        service_uuid = config.get("service_uuid", "")
        if not java_port:
            return

        try:
            # 构建通知数据
            notification = {
                "char_uuid": char_uuid,
                "service_uuid": service_uuid,
                "data": list(data)  # 转为列表便于JSON序列化
            }
            message = json.dumps(notification) + "\n"

            # 建立临时Socket连接推送通知
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.connect(("127.0.0.1", java_port))
                sock.sendall(message.encode("utf-8"))
        except Exception as e:
            print(f"[Notification Push Failed] {char_uuid}: {e}", flush=True)

    def handle_unsubscribe(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """取消订阅"""
        if not self.client or not self.client.is_connected:
            return {"success": False, "error": "Not connected"}

        char_uuid = command.get("char_uuid")
        if not char_uuid:
            return {"success": False, "error": "Characteristic UUID required"}

        async def unsubscribe():
            await self.client.stop_notify(char_uuid)
            return True

        try:
            self.run_async(unsubscribe())
            # 清理通知配置
            if char_uuid in self.notification_configs:
                del self.notification_configs[char_uuid]
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def handle_is_connected(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """检查连接状态"""
        connected = self.client is not None and self.client.is_connected
        return {"success": True, "connected": connected, "address": self.connected_device}

    def handle_is_scanning(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """检查扫描状态"""
        return {"success": True, "scanning": self.is_scanning}

    def handle_ping(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """心跳检测"""
        return {"success": True, "message": "pong"}

    def stop(self):
        """停止服务"""
        self.running = False
        if self.server_socket:
            self.server_socket.close()
        print("[BLE Bridge] 服务停止", flush=True)


def main():
    port = 8765
    if len(sys.argv) > 1:
        port = int(sys.argv[1])

    service = BleBridgeService(port)
    try:
        service.start()
    except KeyboardInterrupt:
        print("\n[BLE Bridge] 收到中断信号", flush=True)
    finally:
        service.stop()


if __name__ == "__main__":
    main()
