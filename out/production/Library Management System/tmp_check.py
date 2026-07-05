import socket
s = socket.socket()
s.settimeout(2)
try:
    s.connect(('127.0.0.1', 3306))
    print('open')
except Exception as e:
    print('closed', e)
finally:
    s.close()
