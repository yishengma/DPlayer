//
// Created by 海盗的帽子 on 2020/6/11.
//

#include "packet_queue.h"
#include "golbal_define.h"

PacketQueue::~PacketQueue() {
    if (packetQueue != NULL) {
        clear();
        delete packetQueue;
        packetQueue = NULL;
    }
    pthread_mutex_destroy(&packetMutex);
    pthread_cond_destroy(&packetCond);

}

PacketQueue::PacketQueue() {
    packetQueue = new std::queue<AVPacket *>();
    pthread_mutex_init(&packetMutex, NULL);
    pthread_cond_init(&packetCond, NULL);
}

void PacketQueue::push(AVPacket *packet) {
    pthread_mutex_lock(&packetMutex);
    packetQueue->push(packet);
    pthread_cond_signal(&packetCond);
    pthread_mutex_unlock(&packetMutex);
}

AVPacket *PacketQueue::pop() {
    AVPacket *packet;
    pthread_mutex_lock(&packetMutex);
    while (packetQueue->empty()) {
        pthread_cond_wait(&packetCond, &packetMutex);
    }
    packet = packetQueue->front();
    packetQueue->pop();
    pthread_mutex_unlock(&packetMutex);
    return packet;
}

void PacketQueue::clear() {

}
