#!/usr/bin/env python
# -*- coding: gbk -*-

import socket
import thread
import autopy
# from win32api import keybd_event

global tstop

tstop = False

def respinspector():
    '''
    respond udp package from client inspector
    '''
    addr = ('', 7732)
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)  
    s.bind(addr)  
      
    while not tstop:  
        data, recvaddr = s.recvfrom(1024)
        if data=='ankc':
            print 'server discovery respond is sent.'
            s.sendto('anks',recvaddr)
    s.close()
    thread.exit_thread()

keymap = {'0':'0','1':'1','2':'2','3':'3','4':'4','5':'5','6':'6','7':'7','8':'8','9':'9','e':autopy.key.K_RETURN}
def dokey(orikey):
    '''
    simulate keypress event
    '''
    key = keymap.get(orikey)
    if key:
        print 'key event (%s)' % orikey
        autopy.key.tap(key)

if __name__ == '__main__':
    # show local ip
    # localIP = socket.gethostbyname(socket.gethostname())
    # print 'local ip is %s' % localIP
    
    # start respond inspector as a new thread
    thread.start_new_thread(respinspector, ())

    addr = ('', 7732)
    
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.bind(addr)
    sock.listen(5)
    
    while True:
        print 'waiting for connecting...'
        connection, recvaddr = sock.accept()
        print 'connected from:', recvaddr
        
        while True:
            data = connection.recv(1024)
            if data:
                if data == 'q':
                    print 'The server is stopping now!'
                    tstop = True
                    break
                else:
                    dokey(data)
        connection.close()
    
    sock.close()
