#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BLE GUI Tool - Standalone BLE Tool with GUI
AI直接通过Socket与此进程通信，Java只负责启动此进程
"""

import asyncio
import json
import sys
import threading
import socket
import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox
from typing import Optional, Dict, Any, List
from datetime import datetime
from queue import Queue
from bleak import BleakClient, BleakScanner, BleakError
from bleak.backends.characteristic import BleakGATTCharacteristic


class BleGuiTool:
    """BLE GUI工具 - AI直接通过Socket通信"""

    def __init__(self, service_port: int = 8765, version: str = "255.0.0"):
        self.service_port = service_port
        self.version = version
        self.server_socket: Optional[socket.socket] = None
        self.running = False
        self.client: Optional[BleakClient] = None
        self.connected_device: Optional[str] = None
        self.is_scanning: bool = False
        self.scanned_devices: Dict[str, Dict] = {}

        # 缓冲区（存储接收到的通知数据）
        self.data_buffer: List[str] = []
        self.max_buffer_lines = 10000
        self.display_as_hex = False

        # GUI组件
        self.root: Optional[tk.Tk] = None
        self.log_area: Optional[scrolledtext.ScrolledText] = None
        self.status_label: Optional[ttk.Label] = None

        # 控制面板组件
        self.device_combo: Optional[ttk.Combobox] = None
        self.scan_button: Optional[ttk.Button] = None
        self.connect_status_label: Optional[tk.Label] = None
        self.connect_button: Optional[ttk.Button] = None
        self.disconnect_button: Optional[ttk.Button] = None

        # 后台事件循环（用于保持通知接收）
        self._loop: Optional[asyncio.AbstractEventLoop] = None
        self._loop_thread: Optional[threading.Thread] = None

        # 日志队列（用于线程安全地更新UI）
        self._log_queue: Queue = Queue()

        # 通知回调注册表（保持对回调函数的强引用）
        self._notification_callbacks: Dict[str, Any] = {}

    def _get_ble_version(self) -> str:
        """获取BLE库版本号"""
        try:
            import bleak
            return f"bleak {bleak.__version__}"
        except Exception:
            return "bleak 未知"

    def log(self, message: str, tag: str = "INFO"):
        """添加日志 - 线程安全的方式"""
        timestamp = datetime.now().strftime("%H:%M:%S.%f")[:-3]
        log_line = f"[{timestamp}] [{tag}] {message}"
        print(log_line, flush=True)

        # 将日志放入队列，等待主线程处理
        self._log_queue.put(log_line)

    def _process_log_queue(self):
        """处理日志队列 - 在主线程中定期调用"""
        try:
            while not self._log_queue.empty():
                log_line = self._log_queue.get_nowait()
                if self.log_area:
                    self.log_area.insert(tk.END, log_line + "\n")
                    self.log_area.see(tk.END)
                    # 限制行数
                    try:
                        line_count = int(self.log_area.index('end-1c').split('.')[0])
                        if line_count > self.max_buffer_lines:
                            self.log_area.delete('1.0', '100.0')
                    except (ValueError, IndexError):
                        pass
        except Exception as e:
            print(f"[ERROR] Process log queue error: {e}", flush=True)

        # 继续定时检查
        if self.root:
            self.root.after(100, self._process_log_queue)

    def create_gui(self):
        """创建GUI窗口 - 仿照串口工具设计"""
        self.root = tk.Tk()
        self.root.title(f"BLE蓝牙工具 - 端口:{self.service_port}")
        self.root.geometry("900x600")
        self.root.minsize(800, 500)

        # Set window icon
        try:
            import os
            icon_path = os.path.join(os.path.dirname(__file__), '..', 'image', 'logo.ico')
            icon_path = os.path.abspath(icon_path)
            if os.path.exists(icon_path):
                self.root.iconbitmap(icon_path)
        except Exception:
            pass

        # 主面板
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.pack(fill=tk.BOTH, expand=True)

        # ========== 顶部控制面板 ==========
        control_frame = ttk.LabelFrame(main_frame, text="BLE设备", padding="10")
        control_frame.pack(fill=tk.X, pady=(0, 5))

        # 设备选择下拉框
        ttk.Label(control_frame, text="设备:").pack(side=tk.LEFT, padx=(0, 5))
        self.device_combo = ttk.Combobox(control_frame, width=50, state="readonly")
        self.device_combo.pack(side=tk.LEFT, padx=5)
        self.device_combo["values"] = ["点击扫描搜索BLE设备..."]
        self.device_combo.current(0)

        # 刷新/扫描按钮
        self.scan_button = ttk.Button(control_frame, text="扫描", command=self.gui_scan, width=8)
        self.scan_button.pack(side=tk.LEFT, padx=5)

        # 连接状态指示（彩色标签）
        self.connect_status_label = tk.Label(control_frame, text="● 未连接", fg="gray", font=('Arial', 10, 'bold'))
        self.connect_status_label.pack(side=tk.LEFT, padx=(20, 5))

        # 连接/断开按钮
        self.connect_button = ttk.Button(control_frame, text="连接", command=self.gui_connect, width=8)
        self.connect_button.pack(side=tk.LEFT, padx=5)

        # 断开按钮（初始禁用）
        self.disconnect_button = ttk.Button(control_frame, text="断开", command=self.gui_disconnect, width=8, state="disabled")
        self.disconnect_button.pack(side=tk.LEFT, padx=5)

        # 服务端口显示
        ttk.Label(control_frame, text=f"端口:{self.service_port}", foreground="gray").pack(side=tk.RIGHT)

        # ========== 通信日志区域 ==========
        log_frame = ttk.LabelFrame(main_frame, text="通信日志", padding="5")
        log_frame.pack(fill=tk.BOTH, expand=True, pady=5)

        self.log_area = scrolledtext.ScrolledText(log_frame, wrap=tk.WORD, font=('Consolas', 10))
        self.log_area.pack(fill=tk.BOTH, expand=True)

        # ========== 底部状态栏 ==========
        bottom_frame = ttk.Frame(main_frame)
        bottom_frame.pack(fill=tk.X, pady=(5, 0))

        self.status_label = ttk.Label(bottom_frame, text="就绪", foreground="gray")
        self.status_label.pack(side=tk.LEFT)

        ttk.Button(bottom_frame, text="清空日志", command=self.clear_log).pack(side=tk.RIGHT)

        # ========== 作者信息栏 ==========
        author_frame = tk.Frame(main_frame, bg="#f0f0f0")
        author_frame.pack(fill=tk.X, pady=(5, 0))

        # 作者
        author_label = tk.Label(author_frame, text="作者：鸫楼桥鹊",
                                font=('Microsoft YaHei', 9),
                                fg="#555555", bg="#f0f0f0")
        author_label.pack(side=tk.LEFT, padx=10)

        # 版本
        version_text = f"V{self.version}" if self.version and self.version != "无法获取" else "无法获取"
        version_label = tk.Label(author_frame, text=f"版本：{version_text}",
                                font=('Consolas', 9),
                                fg="#555555", bg="#f0f0f0")
        version_label.pack(side=tk.LEFT, padx=20)

        # BLE库版本
        ble_version = self._get_ble_version()
        ble_version_label = tk.Label(author_frame, text=f"BLE库：{ble_version}",
                                font=('Consolas', 9),
                                fg="#555555", bg="#f0f0f0")
        ble_version_label.pack(side=tk.LEFT, padx=20)

        # 联系作者（可点击链接）
        def open_website(event):
            import webbrowser
            webbrowser.open("http://www.donglouqiaoque.com")

        link_label = tk.Label(author_frame, text="联系作者：www.donglouqiaoque.com", 
                            font=('Microsoft YaHei', 9, 'underline'), 
                            fg="#0066cc", bg="#f0f0f0", cursor="hand2")
        link_label.pack(side=tk.LEFT, padx=20)
        link_label.bind("<Button-1>", open_website)
        link_label.bind("<Enter>", lambda e: link_label.config(fg="#003399"))
        link_label.bind("<Leave>", lambda e: link_label.config(fg="#0066cc"))

        # 关闭处理
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)

        self.log("BLE蓝牙工具启动完成", "SYS")

    def gui_scan(self):
        """GUI扫描按钮"""
        if self.is_scanning:
            return
        threading.Thread(target=self._do_scan_sync, daemon=True).start()

    def _do_scan_sync(self):
        """执行扫描"""
        try:
            self.is_scanning = True
            self.update_status("正在扫描...")
            self.log("开始扫描BLE设备...", "SCAN")

            # 禁用扫描按钮
            if self.root:
                self.root.after(0, lambda: self.scan_button.config(state="disabled"))

            devices = asyncio.run(BleakScanner.discover(timeout=10.0))

            self.scanned_devices.clear()
            device_list = []

            for device in devices:
                if device.name and device.name != "Unknown":
                    rssi = getattr(device, 'rssi', -100)
                    self.scanned_devices[device.address] = {
                        "name": device.name,
                        "rssi": rssi
                    }
                    device_list.append({
                        "address": device.address,
                        "name": device.name,
                        "rssi": rssi
                    })
                    self.log(f"发现设备: {device.name} [{device.address}] {rssi}dBm", "SCAN")

            # 更新下拉列表
            if self.root:
                def update_combo():
                    if len(device_list) == 0:
                        self.device_combo["values"] = ["未发现BLE设备"]
                        self.device_combo.current(0)
                    else:
                        combo_items = []
                        for d in device_list:
                            item = f"{d['name']} [{d['address']}] {d['rssi']}dBm"
                            combo_items.append(item)
                        self.device_combo["values"] = combo_items
                        self.device_combo.current(0)
                self.root.after(0, update_combo)

            self.log(f"扫描完成，共发现 {len(device_list)} 个设备", "SCAN")

        except Exception as e:
            self.log(f"扫描失败: {e}", "ERROR")
        finally:
            self.is_scanning = False
            self.update_status("就绪")
            # 启用扫描按钮
            if self.root:
                self.root.after(0, lambda: self.scan_button.config(state="normal"))

    def update_status(self, status: str):
        """更新状态栏"""
        if self.status_label and self.root:
            self.root.after(0, lambda: self.status_label.config(text=status))

    def update_connection_status(self, connected: bool, device_address: str = None):
        """更新连接状态显示"""
        if self.root:
            def _update():
                if connected:
                    self.connect_status_label.config(text="● 已连接", fg="green")
                    self.connect_button.config(state="disabled")
                    self.disconnect_button.config(state="normal")
                    self.scan_button.config(state="disabled")
                    short_addr = device_address.split(':')[-2:] if device_address else ["?", "?"]
                    self.status_label.config(text=f"已连接: {':'.join(short_addr)}")
                else:
                    self.connect_status_label.config(text="● 未连接", fg="gray")
                    self.connect_button.config(state="normal")
                    self.disconnect_button.config(state="disabled")
                    self.scan_button.config(state="normal")
                    self.status_label.config(text="就绪")
            self.root.after(0, _update)

    def gui_connect(self):
        """GUI连接按钮"""
        selected = self.device_combo.get()
        if not selected or selected in ["点击扫描搜索BLE设备...", "未发现BLE设备"]:
            self.log("请先扫描并选择设备", "WARN")
            return

        # 从格式 "DeviceName [AA:BB:CC:DD:EE:FF] -XXdBm" 解析地址
        try:
            addr_start = selected.find('[')
            addr_end = selected.find(']')
            if addr_start > 0 and addr_end > addr_start:
                address = selected[addr_start + 1:addr_end]
                threading.Thread(target=self._do_connect, args=(address,), daemon=True).start()
        except Exception as e:
            self.log(f"解析设备地址失败: {e}", "ERROR")

    def _do_connect(self, address: str):
        """执行连接"""
        try:
            self.update_status("正在连接...")
            self.log(f"正在连接 {address}...", "CONN")

            self.client = BleakClient(address)
            connected = asyncio.run(self.client.connect())

            if connected:
                self.connected_device = address
                self.log(f"已连接到 {address}", "CONN")
                self.update_connection_status(True, address)
                # 启动后台事件循环用于通知接收
                self._start_background_loop()
            else:
                self.log("连接失败", "ERROR")
                self.update_connection_status(False)
        except Exception as e:
            self.log(f"连接失败: {e}", "ERROR")
            self.update_connection_status(False)

    def _start_background_loop(self):
        """启动后台事件循环，用于接收通知"""
        # 检查是否需要启动或重新启动事件循环
        if self._loop is None or not self._loop.is_running():
            # 如果之前有旧的循环，先停止它
            if self._loop is not None and self._loop_thread is not None:
                try:
                    self._loop.call_soon_threadsafe(self._loop.stop)
                    self._loop_thread.join(timeout=2)
                except:
                    pass
            
            # 创建新的事件循环
            self._loop = asyncio.new_event_loop()
            self._loop_thread = threading.Thread(target=self._run_loop_forever, daemon=True)
            self._loop_thread.start()
            self.log("后台事件循环已启动", "SYS")

    def _run_loop_forever(self):
        """在后台线程中运行事件循环"""
        asyncio.set_event_loop(self._loop)
        self._loop.run_forever()

    def _run_in_loop(self, coro):
        """在后台事件循环中运行协程"""
        if self._loop and self._loop.is_running():
            future = asyncio.run_coroutine_threadsafe(coro, self._loop)
            try:
                result = future.result(timeout=10)
                # 如果协程返回 None，表示成功
                if result is None:
                    return {"success": True}
                return result
            except Exception as e:
                return {"success": False, "error": str(e)}
        else:
            return {"success": False, "error": "Event loop not running"}

    def _stop_background_loop(self):
        """停止后台事件循环"""
        if self._loop and self._loop.is_running():
            self._loop.call_soon_threadsafe(self._loop.stop)
            self._loop_thread.join(timeout=2)
            self._loop = None
            self._loop_thread = None
            self.log("后台事件循环已停止", "SYS")

    def gui_disconnect(self):
        """GUI断开按钮"""
        threading.Thread(target=self._do_disconnect, daemon=True).start()

    def _do_disconnect(self):
        """执行断开"""
        try:
            # 先停止后台事件循环
            self._stop_background_loop()
            if self.client and self.client.is_connected:
                asyncio.run(self.client.disconnect())
            self.client = None
            self.connected_device = None
            self.log("已断开连接", "CONN")
            self.update_connection_status(False)
        except Exception as e:
            self.log(f"断开连接失败: {e}", "ERROR")

    def clear_log(self):
        """清空日志"""
        if self.log_area:
            self.log_area.delete(1.0, tk.END)

    def on_close(self):
        """窗口关闭"""
        self.log("正在关闭...", "SYS")
        self.running = False
        self.stop()
        if self.root:
            self.root.destroy()
        sys.exit(0)

    # ==================== Socket服务 ====================

    def start_server(self):
        """启动Socket服务"""
        def server_loop():
            try:
                self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                self.server_socket.bind(('127.0.0.1', self.service_port))
                self.server_socket.listen(5)
                self.running = True

                self.log(f"Socket服务启动，监听端口: {self.service_port}", "SYS")

                while self.running:
                    try:
                        self.server_socket.settimeout(1.0)
                        conn, addr = self.server_socket.accept()
                        self.log(f"AI客户端连接: {addr}", "CONN")
                        threading.Thread(target=self.handle_client, args=(conn,), daemon=True).start()
                    except socket.timeout:
                        continue
                    except Exception as e:
                        if self.running:
                            self.log(f"Socket错误: {e}", "ERROR")
            except Exception as e:
                self.log(f"服务启动失败: {e}", "ERROR")

        threading.Thread(target=server_loop, daemon=True).start()

    def handle_client(self, conn: socket.socket):
        """处理AI客户端连接"""
        try:
            while self.running:
                # 读取命令
                data = self._recv_line(conn)
                if not data:
                    break

                self.log(f"收到命令: {data[:100]}...", "CMD")
                result = self.execute_command(data)
                response = json.dumps(result) + '\n'
                conn.sendall(response.encode('utf-8'))
                self.log(f"返回结果: {str(result)[:100]}...", "RESP")
        except Exception as e:
            self.log(f"客户端处理异常: {e}", "ERROR")
        finally:
            conn.close()
            self.log("AI客户端断开", "CONN")

    def _recv_line(self, conn: socket.socket) -> str:
        """接收一行数据"""
        buffer = b""
        while self.running:
            try:
                conn.settimeout(30.0)
                chunk = conn.recv(4096)
                if not chunk:
                    return ""
                buffer += chunk
                if b'\n' in buffer:
                    break
            except socket.timeout:
                continue
            except Exception:
                return ""
        return buffer.decode('utf-8').strip()

    def execute_command(self, data: str) -> Dict[str, Any]:
        """执行AI命令"""
        try:
            command = json.loads(data)
            cmd_type = command.get("type", "")

            handlers = {
                "ping": lambda c: {"success": True, "message": "pong"},
                "exit": lambda c: self.handle_exit(),
                "scan": self.handle_scan,
                "get_devices": self.handle_get_devices,
                "connect": self.handle_connect,
                "disconnect": self.handle_disconnect,
                "discover": self.handle_discover,
                "read": self.handle_read,
                "write": self.handle_write,
                "subscribe": self.handle_subscribe,
                "is_scanning": lambda c: {"success": True, "scanning": self.is_scanning},
                "is_connected": lambda c: {"success": True, "connected": self.is_connected()},
                "get_log": self.handle_get_log,
                "clear": self.handle_clear,
                "switch_format": self.handle_switch_format,
            }

            handler = handlers.get(cmd_type)
            if handler:
                return handler(command)
            return {"success": False, "error": f"Unknown command: {cmd_type}"}
        except Exception as e:
            return {"success": False, "error": str(e)}

    # ==================== 命令处理 ====================

    def handle_scan(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """扫描设备"""
        if self.is_scanning:
            return {"success": False, "error": "Already scanning"}

        # 获取超时时间（秒），支持字符串和数字
        timeout_sec = command.get("timeout", 10)
        if isinstance(timeout_sec, str):
            try:
                timeout_sec = float(timeout_sec)
            except (ValueError, TypeError):
                timeout_sec = 10.0
        elif isinstance(timeout_sec, int):
            timeout_sec = float(timeout_sec)
        elif not isinstance(timeout_sec, float):
            timeout_sec = 10.0

        self.log(f"扫描参数: timeout={timeout_sec} (type: {type(timeout_sec).__name__})", "DEBUG")

        def do_scan():
            try:
                self.is_scanning = True
                self.log(f"开始扫描（超时{timeout_sec}s）...", "SCAN")

                devices = asyncio.run(BleakScanner.discover(timeout=timeout_sec))

                self.scanned_devices.clear()
                for device in devices:
                    if device.name and device.name != "Unknown":
                        rssi = getattr(device, 'rssi', -100)
                        self.scanned_devices[device.address] = {
                            "name": device.name,
                            "rssi": rssi
                        }

                # 更新GUI下拉列表
                if self.root and self.device_combo:
                    def update_combo():
                        if len(self.scanned_devices) == 0:
                            self.device_combo["values"] = ["未发现BLE设备"]
                            self.device_combo.current(0)
                        else:
                            combo_items = []
                            for addr, info in self.scanned_devices.items():
                                item = f"{info['name']} [{addr}] {info['rssi']}dBm"
                                combo_items.append(item)
                            self.device_combo["values"] = combo_items
                            self.device_combo.current(0)
                    self.root.after(0, update_combo)

                self.log(f"扫描完成，发现 {len(self.scanned_devices)} 个设备", "SCAN")
            except Exception as e:
                self.log(f"扫描失败: {e}", "ERROR")
            finally:
                self.is_scanning = False

        threading.Thread(target=do_scan, daemon=True).start()
        return {"success": True, "message": "Scanning started"}

    def handle_exit(self) -> Dict[str, Any]:
        """退出程序"""
        self.log("收到退出命令", "SYS")
        threading.Thread(target=self.on_close, daemon=True).start()
        return {"success": True, "message": "Exiting"}

    def handle_get_devices(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """获取扫描到的设备列表"""
        devices = []
        for addr, info in self.scanned_devices.items():
            devices.append({
                "address": addr,
                "name": info["name"],
                "rssi": info.get("rssi", -100)
            })
        return {"success": True, "devices": devices}

    def handle_connect(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """连接设备"""
        address = command.get("address")
        if not address:
            return {"success": False, "error": "Address required"}

        if self.client and self.client.is_connected:
            return {"success": False, "error": "Already connected"}

        def do_connect():
            try:
                self.log(f"正在连接 {address}...", "CONN")
                self.client = BleakClient(address)
                connected = asyncio.run(self.client.connect())

                if connected:
                    self.connected_device = address
                    self.log(f"已连接到 {address}", "CONN")
                    self.update_connection_status(True, address)
                    # 启动后台事件循环用于通知接收
                    self._start_background_loop()
                else:
                    self.log("连接失败", "ERROR")
                    self.update_connection_status(False)
            except Exception as e:
                self.log(f"连接异常: {e}", "ERROR")

        threading.Thread(target=do_connect, daemon=True).start()
        return {"success": True, "message": "Connecting..."}

    def handle_disconnect(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """断开连接"""
        if not self.client or not self.client.is_connected:
            return {"success": True, "message": "Not connected"}

        try:
            asyncio.run(self.client.disconnect())
            self.client = None
            self.connected_device = None
            self.log("已断开连接", "CONN")
            self.update_connection_status(False)
            return {"success": True}
        except Exception as e:
            self.update_connection_status(False)
            return {"success": False, "error": str(e)}

    def handle_discover(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """发现服务"""
        if not self.client or not self.client.is_connected:
            return {"success": False, "error": "Not connected"}

        try:
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
            return {"success": True, "services": services}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def handle_read(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """读取特征"""
        if not self.client or not self.client.is_connected:
            return {"success": False, "error": "Not connected"}

        char_uuid = command.get("char_uuid")
        if not char_uuid:
            return {"success": False, "error": "Characteristic UUID required"}

        try:
            data = asyncio.run(self.client.read_gatt_char(char_uuid))
            return {
                "success": True,
                "data": list(data),
                "hex": data.hex(),
                "text": data.decode('utf-8', errors='ignore'),
            }
        except Exception as e:
            return {"success": False, "error": str(e)}

    def handle_write(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """写入特征"""
        if not self.client or not self.client.is_connected:
            return {"success": False, "error": "Not connected"}

        char_uuid = command.get("char_uuid")
        data = command.get("data")

        if not char_uuid or data is None:
            return {"success": False, "error": "UUID and data required"}

        try:
            import asyncio
            byte_data = bytes(data) if isinstance(data, list) else data.encode('utf-8')
            asyncio.run(asyncio.wait_for(
                self.client.write_gatt_char(char_uuid, byte_data),
                timeout=10.0
            ))
            return {"success": True}
        except asyncio.TimeoutError:
            return {"success": False, "error": "Write timeout (10s)"}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def _create_notification_callback(self, char_uuid: str):
        """创建通知回调函数 - 保持强引用避免被GC"""
        def callback(sender, data):
            try:
                hex_str = data.hex()
                text = data.decode('utf-8', errors='ignore')
                log_line = f"[{datetime.now().strftime('%H:%M:%S.%f')[:-3]}] [NOTIFY] {char_uuid}: {hex_str}"
                self.data_buffer.append(log_line)
                self.log(f"[Notify] {char_uuid}: {hex_str} | {text}", "NOTIFY")
            except Exception as e:
                print(f"[ERROR] Notification callback error: {e}", flush=True)
        return callback

    def handle_subscribe(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """订阅通知"""
        if not self.client or not self.client.is_connected:
            return {"success": False, "error": "Not connected"}

        char_uuid = command.get("char_uuid")
        subscribe = command.get("subscribe", True)
        # 支持字符串和布尔值
        if isinstance(subscribe, str):
            subscribe = subscribe.lower() in ('true', '1', 'yes', 'on')
        elif isinstance(subscribe, (int, float)):
            subscribe = bool(subscribe)

        if not char_uuid:
            return {"success": False, "error": "UUID required"}

        # 确保后台事件循环已启动
        self._start_background_loop()

        try:
            if subscribe:
                # 创建回调并保存到注册表（保持强引用）
                callback = self._create_notification_callback(char_uuid)
                self._notification_callbacks[char_uuid] = callback

                # 使用后台事件循环启动通知
                result = self._run_in_loop(self.client.start_notify(char_uuid, callback))
                if isinstance(result, dict) and not result.get("success", True):
                    return result
                self.log(f"通知订阅已启动: {char_uuid}", "INFO")
            else:
                # 使用后台事件循环停止通知
                result = self._run_in_loop(self.client.stop_notify(char_uuid))
                # 从注册表中移除回调
                if char_uuid in self._notification_callbacks:
                    del self._notification_callbacks[char_uuid]
                if isinstance(result, dict) and not result.get("success", True):
                    return result
                self.log(f"通知已取消: {char_uuid}", "INFO")

            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def handle_get_log(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """获取日志"""
        lines = command.get("lines", 50)
        # 支持字符串和数字
        if isinstance(lines, str):
            try:
                lines = int(lines)
            except (ValueError, TypeError):
                lines = 50
        elif isinstance(lines, (int, float)):
            lines = int(lines)
        else:
            lines = 50
        logs = self.data_buffer[-lines:] if self.data_buffer else []
        return {"success": True, "logs": logs}

    def handle_clear(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """清空缓冲区"""
        self.data_buffer.clear()
        return {"success": True}

    def handle_switch_format(self, command: Dict[str, Any]) -> Dict[str, Any]:
        """切换显示格式"""
        self.display_as_hex = command.get("format", "text") == "hex"
        return {"success": True, "format": "hex" if self.display_as_hex else "text"}

    def is_connected(self) -> bool:
        """是否已连接"""
        return self.client is not None and self.client.is_connected

    def stop(self):
        """停止服务"""
        self.running = False
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass

    def run(self):
        """运行"""
        self.create_gui()
        self.start_server()
        # 启动日志队列处理
        self._process_log_queue()
        self.root.mainloop()


def main():
    """主函数"""
    import argparse
    parser = argparse.ArgumentParser(description='BLE GUI Tool')
    parser.add_argument('--port', type=int, default=8765, help='Service port')
    parser.add_argument('--version', type=str, default='255.0.0', help='App version')
    parser.add_argument('--build-date', type=str, default='unknown', help='Build date')
    parser.add_argument('--build-time', type=str, default='unknown', help='Build time')
    args = parser.parse_args()

    # 组合版本信息
    version_str = args.version
    if args.build_date != 'unknown' and args.build_time != 'unknown':
        version_str = f"{args.version}（{args.build_date} {args.build_time}）"

    app = BleGuiTool(service_port=args.port, version=version_str)
    app.run()


if __name__ == "__main__":
    main()
