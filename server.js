const express = require('express');
const http = require('http');
const { WebSocketServer } = require('ws');
const path = require('path');

const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server });

// التوجيه الصحيح: قراءة ملف index.html وإرساله للمتصفح فوراً
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

// إدارة اتصالات الـ WebSocket للبث المباشر
wss.on('connection', (ws) => {
    console.log('New client connected');
    
    ws.on('message', (message) => {
        wss.clients.forEach((client) => {
            if (client !== ws && client.readyState === ws.OPEN) {
                client.send(message);
            }
        });
    });
});

// تشغيل السيرفر على البورت المتاح
server.listen(process.env.PORT || 3000, () => {
    console.log('Server listening...');
});
