const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const path = require('path');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

// البحث عن الملف في نفس مجلد السيرفر
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

wss.on('connection', (ws) => {
    console.log('New connection established');
    ws.on('message', (data) => {
        // إذا كانت البيانات نصية (JSON) نرسلها كما هي
        if (typeof data === 'string' || Buffer.isBuffer(data)) {
            let messageStr = data.toString();
            if (messageStr.startsWith('{')) {
                wss.clients.forEach((client) => {
                    if (client !== ws && client.readyState === WebSocket.OPEN) {
                        client.send(messageStr);
                    }
                });
                return;
            }
        }

        // توجيه بيانات صور الشاشة للجميع
        wss.clients.forEach((client) => {
            if (client !== ws && client.readyState === WebSocket.OPEN) {
                client.send(data);
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => console.log('Server is live!'));
