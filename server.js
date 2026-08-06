const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const path = require('path');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

app.use(express.static(path.join(__dirname)));

app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

wss.on('connection', (ws) => {
    console.log('New device connected');
    ws.isAlive = true;

    // الحفاظ على الاتصال نشطاً
    ws.on('pong', () => { ws.isAlive = true; });

    ws.on('message', (message) => {
        // بث البيانات لجميع الأجهزة الأخرى
        wss.clients.forEach((client) => {
            if (client !== ws && client.readyState === WebSocket.OPEN) {
                client.send(message);
            }
        });
    });

    ws.on('close', () => console.log('Device disconnected'));
});

// نظام نبضات القلب لمنع انقطاع الاتصال في Render
const interval = setInterval(() => {
    wss.clients.forEach((ws) => {
        if (ws.isAlive === false) return ws.terminate();
        ws.isAlive = false;
        ws.ping();
    });
}, 30000);

wss.on('close', () => clearInterval(interval));

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => console.log(`Server running on port ${PORT}`));
