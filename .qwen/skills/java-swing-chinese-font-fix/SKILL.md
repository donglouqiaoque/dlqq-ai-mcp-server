---
name: java-swing-chinese-font-fix
description: 修复Java Swing对话框中文显示乱码的问题，通过设置支持中文的UI字体
source: auto-skill
extracted_at: '2026-05-31T07:04:47.484Z'
---

# Java Swing 中文乱码修复

## 问题
Swing对话框在Windows系统上默认使用不支持中文的字体，导致中文显示乱码。

## 解决方案

### 1. 添加字体工具方法

```java
/**
 * 检查字体是否可用
 */
private static boolean isFontAvailable(String fontName) {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (String name : ge.getAvailableFontFamilyNames()) {
        if (name.equalsIgnoreCase(fontName)) {
            return true;
        }
    }
    return false;
}

/**
 * 获取支持中文的UI字体
 */
private static Font getChineseFont() {
    // 优先尝试微软雅黑（Windows系统自带）
    if (isFontAvailable("Microsoft YaHei")) {
        return new Font("Microsoft YaHei", Font.PLAIN, 12);
    }
    // 尝试宋体
    if (isFontAvailable("SimSun")) {
        return new Font("SimSun", Font.PLAIN, 12);
    }
    // 回退到Dialog字体
    return new Font("Dialog", Font.PLAIN, 12);
}

/**
 * 获取支持中文的等宽字体（用于代码编辑区域）
 */
private static Font getMonospacedChineseFont() {
    // 尝试使用 Consolas（Windows等宽字体，支持中文）
    if (isFontAvailable("Consolas")) {
        return new Font("Consolas", Font.PLAIN, 12);
    }
    // 尝试使用 Courier New
    if (isFontAvailable("Courier New")) {
        return new Font("Courier New", Font.PLAIN, 12);
    }
    // 回退到 Dialog 字体
    return new Font("Dialog", Font.PLAIN, 12);
}

/**
 * 为组件及其子组件递归设置字体
 */
private static void setFontRecursively(Component comp, Font font) {
    if (comp == null) return;
    comp.setFont(font);
    if (comp instanceof Container) {
        for (Component child : ((Container) comp).getComponents()) {
            setFontRecursively(child, font);
        }
    }
}
```

### 2. 设置全局中文字体（必须在setLookAndFeel之前）

**关键：字体设置必须在 `UIManager.setLookAndFeel()` 之前调用！**

```java
/**
 * 设置全局中文字体 - 必须在设置LookAndFeel之前调用
 */
private static void setupChineseFont() {
    Font uiFont = getChineseFont();

    // 设置所有Swing组件的字体（30+个组件）
    UIManager.put("CheckBox.font", uiFont);
    UIManager.put("CheckBoxMenuItem.font", uiFont);
    UIManager.put("ColorChooser.font", uiFont);
    UIManager.put("ComboBox.font", uiFont);
    UIManager.put("DesktopIcon.font", uiFont);
    UIManager.put("EditorPane.font", uiFont);
    UIManager.put("FormattedTextField.font", uiFont);
    UIManager.put("InternalFrame.titleFont", uiFont);
    UIManager.put("Label.font", uiFont);
    UIManager.put("List.font", uiFont);
    UIManager.put("Menu.font", uiFont);
    UIManager.put("MenuBar.font", uiFont);
    UIManager.put("MenuItem.font", uiFont);
    UIManager.put("OptionPane.buttonFont", uiFont);
    UIManager.put("OptionPane.font", uiFont);
    UIManager.put("OptionPane.messageFont", uiFont);
    UIManager.put("Panel.font", uiFont);
    UIManager.put("PasswordField.font", uiFont);
    UIManager.put("PopupMenu.font", uiFont);
    UIManager.put("ProgressBar.font", uiFont);
    UIManager.put("RadioButton.font", uiFont);
    UIManager.put("RadioButtonMenuItem.font", uiFont);
    UIManager.put("ScrollPane.font", uiFont);
    UIManager.put("Slider.font", uiFont);
    UIManager.put("Spinner.font", uiFont);
    UIManager.put("TabbedPane.font", uiFont);
    UIManager.put("Table.font", uiFont);
    UIManager.put("TableHeader.font", uiFont);
    UIManager.put("TextArea.font", new Font("Monospaced", Font.PLAIN, 12));
    UIManager.put("TextField.font", uiFont);
    UIManager.put("TextPane.font", uiFont);
    UIManager.put("TitledBorder.font", uiFont);
    UIManager.put("ToggleButton.font", uiFont);
    UIManager.put("ToolBar.font", uiFont);
    UIManager.put("ToolTip.font", uiFont);
    UIManager.put("Tree.font", uiFont);
    UIManager.put("Viewport.font", uiFont);
    UIManager.put("Button.font", uiFont);
}
```

### 3. 在对话框方法中正确使用

```java
SwingUtilities.invokeLater(() -> {
    try {
        // 1. 先设置全局字体（必须在setLookAndFeel之前）
        setupChineseFont();

        // 2. 设置外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // 3. 创建自定义JDialog时，额外递归设置字体
        JDialog dialog = new JDialog((Frame) null, title, true);
        // ... 配置对话框 ...
        
        // 为整个对话框递归设置字体
        Font chineseFont = getChineseFont();
        setFontRecursively(dialog, chineseFont);

        dialog.setVisible(true);
    } catch (Exception ex) {
        // 处理异常
    }
});
```

### 4. 覆盖范围

需要对以下所有使用Swing对话框的方法进行修复：
- 自定义JDialog（输入对话框）
- JFileChooser（文件选择器）
- JOptionPane.showConfirmDialog（确认对话框）
- JOptionPane.showMessageDialog（消息对话框）

## 关键要点（重要！）

1. **字体设置必须在 `setLookAndFeel()` 之前** - 这是最关键的一步！LookAndFeel 会基于 UIManager 的字体配置创建组件。

2. **全面覆盖组件字体** - 需要设置 30+ 个 Swing 组件的字体，不只是常用的 Label/Button。

3. **自定义 JDialog 需要递归设置** - `UIManager.put()` 对已创建的组件不生效，需要遍历设置。

4. **字体回退策略** - 优先 Microsoft YaHei → SimSun → Dialog。

5. **文本编辑区域** - TextArea 可以保持 Monospaced 字体（代码编辑场景）。

## 常见错误

❌ **错误做法**（字体设置太晚）：
```java
UIManager.setLookAndFeel(...);  // LookAndFeel已缓存默认字体
setupChineseFont();             // 对已缓存的组件无效
JOptionPane.showMessageDialog(...);  // 还是乱码
```

✅ **正确做法**（先设置字体）：
```java
setupChineseFont();             // 先设置字体配置
UIManager.setLookAndFeel(...);  // LookAndFeel使用新字体配置
JOptionPane.showMessageDialog(...);  // 正常显示中文
```