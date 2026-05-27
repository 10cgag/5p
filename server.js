const express = require('express');
const http = require('http');
const { WebSocketServer } = require('ws');
const path = require('path');

const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server });

app.get('/', (req, res) => res.sendFile(path.join(__dirname, 'index.html')));

wss.on('connection', (ws) => {
    ws.on('message', (data) => {
        // إذا كانت رسالة لمس، أرسلها لكل المتصلين (أو برمجها لتصل للأندرويد فقط)
        wss.clients.forEach((client) => {
            if (client.readyState === 1) client.send(data);
        });
    });
});

server.listen(process.env.PORT || 3000);
