# Enhancement Order Hints

一个适用于 Minecraft Java 26.1.2、Fabric Loader 0.19.3 的纯客户端模组。作者：ozozorz。

当前版本：1.0.4。

## 功能

- 在铁砧界面右侧显示当前方案的最佳附魔合并顺序。
- 按 `N`，或通过 ModMenu，添加、编辑、删除和选择附魔方案。
- 使用“剑、斧、头盔”等物品类别下拉框；同类物品不区分木、铁、钻石等材质。
- 只显示当前物品类别可以使用的附魔，并以彩色行带连续标示互斥附魔组。
- 直接点击附魔行中的等级按钮进行选择；再次点击已选等级即可取消。
- 支持“更少的经验/等级”和“更少的累积惩罚”两种优化目标。
- 选择互斥附魔时自动移除冲突项，并在界面中给出提示。
- 配置保存在 `.minecraft/config/enhancement-order-hints.json`。

算法采用穷举动态规划，成本模型参考 [iamcal/enchant-order](https://github.com/iamcal/enchant-order)：附魔权重、左右物品既往工作惩罚以及铁砧单次 39 级上限都会参与计算。

## 构建

需要 Java 25：

```powershell
gradlew.bat build
```

输出位于 `build/libs/`。ModMenu 是可选依赖；未安装时快捷键和铁砧面板仍可正常工作。
