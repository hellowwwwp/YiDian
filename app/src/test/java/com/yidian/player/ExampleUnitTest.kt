package com.yidian.player

import com.yidian.player.utils.VideoUtils
import org.junit.Test
import java.io.File

class ExampleUnitTest {

    @Test
    fun test1() {
        val path1 = "/storage/emulated/0/Pictures/WeiXin/wx_camera_1633605182390.mp4"
        val path2 = "Pictures/WeiXin/"
        val dir1 = getParentDir(path1)
        val dir2 = getParentDir(path2)
        println(dir1)
        println(dir2)
    }

    private fun getParentDir(path: String): String {
        if (path.isEmpty()) {
            //路径不合法
            return ""
        }
        val lastSeparatorIndex = path.lastIndexOf(File.separator)
        if (lastSeparatorIndex == -1) {
            //没有父文件夹
            return ""
        }
        val relativePath: String =
            if (lastSeparatorIndex != path.length - 1) {
                //不是相对路径,去掉最后一个/后面的内容,返回一个相对路径
                path.substring(0, lastSeparatorIndex + 1)
            } else {
                //本身就是一个相对路径
                path
            }
        return relativePath.split(File.separator)
            .last {
                it.isNotEmpty()
            }
    }

    @Test
    fun test2() {
        val size: Long = 2340024234
//        val value = (size * 1f) / (1024 * 1024)
//        val s = "${String.format("%.2f", value)}MB"
//        println(s)
//        val s = TimeUtils.getDurationStr(size)
//        println(s)
        val s = VideoUtils.formatFileSize(size)
        println(s)
    }
}