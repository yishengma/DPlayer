//
// Created by 海盗的帽子 on 2020/6/11.
//

#ifndef DPLAYER_PACKET_QUEUE_H
#define DPLAYER_PACKET_QUEUE_H

#include <queue>
#include <pthread.h>

extern "C" {
#include <libavdevice/avdevice.h>
};

class PacketQueue {
public:
    std::queue<AVPacket *> *packetQueue = NULL;
    pthread_mutex_t packetMutex;
    pthread_cond_t packetCond;
public:
    PacketQueue();
    virtual ~PacketQueue();

public:
    void push(AVPacket* packet);
    AVPacket* pop();

    void clear();
};


#endif //DPLAYER_PACKET_QUEUE_H
