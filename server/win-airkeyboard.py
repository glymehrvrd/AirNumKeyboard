# -*- coding: gbk -*-

'''
Created on 2013-8-24

@author: glista
'''

import socket
from time import ctime
import thread
from win32api import keybd_event
from win32gui import FindWindow, SetForegroundWindow

def respinspector():
    '''
    respond UDP package from client inspector
    '''
    addr = ('', 7732)
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)  
    s.bind(addr)  
      
    while True:  
        data, recvaddr = s.recvfrom(1024)
        print data
        if data=='ankc':
            print 'server discovery respond is sent.'
            s.sendto('anks',recvaddr)
    s.close()

    thread.exit_thread()

keymap = {'0':48,'1':49,'2':50,'3':51,'4':52,'5':53,'6':54,'7':55,'8':56,'9':57,'/':111,'*':106,'-':109,'+':107,'e':13,'.':110,'s':27,'n':144}
def dokey(orikey):
    '''
    simulate keypress event
    '''
    hwnd = FindWindow(None, "Anki - User 1")
    SetForegroundWindow(hwnd)
    key = keymap.get(orikey)
    if key:
        print 'key event (%s)' % orikey
        keybd_event(key,0,0,0)

if __name__ == '__main__':
    # show local ip
    localIP = socket.gethostbyname(socket.gethostname())
    print 'local ip is %s' % localIP
    
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
                    break
                else:
                    dokey(data)
        connection.close()
    
    sock.close()
