package com.example.reba.utils

import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer

class MediaPlayUtils {
    companion object {
        private val mediaPlayer = MediaPlayer()
        fun initMediaPlay(fd: AssetFileDescriptor, isLooping: Boolean): MediaPlayer {
            try {
                mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                mediaPlayer.isLooping = isLooping //设置为循环播放
                mediaPlayer.prepareAsync() //初始化播放器MediaPlayer
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return mediaPlayer
        }
    }
}
