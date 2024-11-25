## Shinkeiseikan Collection (yor42 fork)
[![Java CI with Gradle](https://github.com/yor42/ShinColle/actions/workflows/gradle.yml/badge.svg)](https://github.com/yor42/ShinColle/actions/workflows/gradle.yml)
```
yor42 -fork-> misaka10843 -fork-> Cappycot -fork-> PinkaLulan
                   ∧
                  pull
                   |
           KCFindstr、sddsd2332

Thank all the above personnel for their outstanding contributions!
```
This is yor42's unofficial fork of unofficial fork of unofficial fork for shinkeiseikan collection.
Goal of this fork is:<br>
- Migrate workspace to RFG using [Cleanroom Template Dev Env](https://github.com/CleanroomMC/TemplateDevEnv), allowing build of this mod on later gradle.<br>
- Completely fix Cascading Worldgens
- Implemented IC2 compatibility that was left NYI, along with RF compatibility
- Removed slow TESR and migrated them to vanilla JSON model if possible
- Added Korean
- Add compatibility with Mekanism

as always, you can grab build jar on [Github Actions](https://github.com/yor42/ShinColle/actions)<br>
this mod uses part of code and texture of PlusTic by Landmaster; which uses Apache License Version 2.0.

# Shin Colle(Mikasa10843)

此仓库是复刻自 Cappycot/ShinColle后使用GitHub actions自动编译程序编译jar，顺便将其他仓库中的优化与修复添加于此 

当前此仓库仅修复仅开启此mod下的重大bug（并不会主动修复与其他mod兼容的问题），虽然也可以在issue中提出其他的bug，但是并不会主动修复（因为版本对于现在来说太老了，并没有过多的维护价值）

此仓库预计只会支持到mojang发布1.22.2+的时候，之后就算有重大的bug也并不会修复了

```
misaka10843 -fork-> Cappycot -fork-> PinkaLulan
 ∧
pull
 |
KCFindstr、sddsd2332

Thank all the above personnel for their outstanding contributions!
```

这样您就可以无需自己编译，只需要跳转到[这里](https://github.com/misaka10843/ShinColle/actions)点击最新的构建下载即可qwq

如果怕不稳定，您可以点击[这里](https://github.com/misaka10843/ShinColle/releases)下载最新版即可qwq

如果遇见了bug，还请您在[issue](https://github.com/misaka10843/ShinColle/issues)中提交信息，但是因为本人为mc mod小白，所以可能并不会修复，还请您等待大佬进行修复qwq

如需要添加一些新的物品/舰娘，或者单纯想问问或者灌水，可以前往[discussions](https://github.com/misaka10843/ShinColle/discussions)！

如需要体验原配的舰娘语音(虽然很多深海舰无法实装)，您可以在[这里](https://github.com/misaka10843/ShinColle/pull/8)，点击`检查->细节`即可下载到最新的语音版jar

在[这里](https://github.com/misaka10843/ShinColle/blob/mc-1.12.2/cfg-info.md)可以查看配置文件设置

请注意！在市面上1.12.2的搬运实体类型的Mod中，本人测试只通过了[进入水桶](https://www.mcmod.cn/class/2192.html)这个Mod，如果使用其他Mod可能会导致一些问题！

## 正式版的问题测试情况 

|问题|通过状态|测试端|备注|
|  :----:  | :----:  |  :----:  | :----:  |
[攻击问题](https://github.com/misaka10843/ShinColle/pull/2)|✅|本地游戏|Y轴角度太大会出现无法命中情况|
[图鉴不显示](https://github.com/misaka10843/ShinColle/pull/2)|✅|本地游戏|简单测试|
[舰娘在捡起物品时可能造成复制](https://github.com/misaka10843/ShinColle/pull/3#issuecomment-1134689839)|✅|本地游戏|[@KCFindstr](https://github.com/KCFindstr) 修复，简单测试|
[捡他人的舰娘会循环物品拾取音效](https://github.com/misaka10843/ShinColle/issues/9)|✅|多人游戏|简单测试|
[深海日志文字显示一半](https://github.com/misaka10843/ShinColle/issues/12)|✅|多人/本地游戏|无|
[攻击表情修复](https://github.com/misaka10843/ShinColle/pull/2)|❓|暂未测试/已修改|并不知道具体问题|
[Boss无法在玩家死亡后清除](https://github.com/misaka10843/ShinColle/issues/6)|❌|暂未修复(疑似故意编写)|等待大佬PR|
[在方块上放在放置重型的怨念团块后可交互物品变成超深海热漩贴图](https://github.com/misaka10843/ShinColle/issues/14)|❌|暂未修复|等待大佬PR|
[合成表与其他mod不兼容](https://github.com/misaka10843/ShinColle/issues/15)|✅|本地游戏|[@sddsd2332](https://github.com/sddsd2332) 修复，简单测试|
[世界时间固定时，舰船AI报时语音循环播放](https://github.com/misaka10843/ShinColle/issues/16)|✅|本地游戏|[@sddsd2332](https://github.com/sddsd2332) 修复，简单测试|

## 预发布版的一些相关事项

注意！这是我们暂时合并了**修复图鉴不显示、攻击问题与攻击表情修复**的jar，

因为可能会导致一些问题，

所以如果您想使用稳定版，还请下载最新的稳定版，而不是这个预发行版！

如果您想提供信息给我们，

请使用最新的**预发行版**（ [这里](https://github.com/misaka10843/ShinColle/releases/tag/2022.5.22-1) ），

并且发现问题后在[这里](https://github.com/misaka10843/ShinColle/pull/3)进行相关留言

如果出现错误，可以直接切换到[发行版](https://github.com/misaka10843/ShinColle/releases/latest)

## Shinkeiseikan Collection


Based off https://github.com/Theray070696/ShinColle

Technically working 1.12.2 version. Check bugs.txt for a more in-depth coverage of what I did to it so far and known remaining issues. -Mystia

-------------------------------------------------------------

This is an unofficial port of the 1.10.2 build to 1.12.2.
No new features will be added, and only bugs caused by the 1.12.2 port will be fixed.

Add cute ship girls for battle and decoration.


To translator (except zh_TW):
You can edit ship/item description as you like.
I finished en_US by google translate, please correct the weird translation if you found.



Dev Video:
https://www.youtube.com/playlist?list=PLzA0TpkwD25D0Q8bwcejxokKJDVzSnk2A

Forum (Chinese)
http://forum.gamer.com.tw/C.php?bsn=18673&snA=124090&tnum=1

Mod download (Curse)
http://www.curse.com/mc-mods/minecraft/228395-shincolle

Mod source code with build.gradle (Google Doc)
1.7.10:
https://drive.google.com/file/d/0Bwod9-SE78mFbkoydnZFbmtnRDg/view?usp=sharing

