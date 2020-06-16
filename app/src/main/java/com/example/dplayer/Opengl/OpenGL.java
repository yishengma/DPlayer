package com.example.dplayer.Opengl;

//
// Created by 海盗的帽子 on 2020/6/12.
//
public class OpenGL {
    //OpenGL 是一种应用程序的编程接口，他是一种可以对图像硬件设备进行访问的软件库，OpenGL 被设计为一个现代化的，硬件接口无关的接口
    //因此我们不需要考虑操作系统的前提下，在多种不同的图像硬件系统上通过软件的形式的实现 OpenGl 接口
    //OpenGL 也没有提供任何表达三维物体模型和读取图文操作

    //作用
    //视频，图形，图片处理，2D 和 3D,游戏引擎，CAD , 虚拟现实，AI 人工智能

    //OpenGLES 是 OpenGL 精简版本

    //demo
    //https://github.com/android/ndk-samples/tree/master/gles3jni
    //基础绘制，纹理，模型，雾效果，相机，美容，native 层

    //ViewRootImpl
    //Surface
    //Canvas canvas = surface.lock();
    //把 canvas 传给 view
    //Ques
    //SurfaceView 为什么能在子线程更新
    //SurfaceView 的挖洞原理（显示机制） SurfaceFlin



    //怎么渲染一张图
    //1.继承GLSurfaceView ,设置版本 2
    //2.继承渲染器BitmapRender
    //3.写顶点坐标和纹理
    //4.编译链接生成程序
    //5.生成记载Bitmap 的纹理
    //6.绘制到屏幕

    //屏幕            OpenGL               GPU
    //屏幕的渲染与Surface 有关（Canvas 从Surface拿）
    //OpenGL ：OpenGL 环境 GLContext ,GLSurface ,纹理
    //通过 GLSurface 与屏幕的 Surface 绑定
    //EGLContext 将数据交给GPU,GPU 返回GLSurface
    //GLSurface将数据同步到屏幕的 Surface



    //OpenGL 的坐标
    //   （-1，1），中间为 （0，0）

    //纹理的坐标
    //（0，1）
    //（0，1）
    //左上角为（0，0）
}
