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
        let messageString = data.toString();
        
        // محاولة فحص هل الرسالة إحداثيات لمس (JSON) أم صورة (Binary)
        try {
            const parsed = JSON.parse(messageString);
            if (parsed.type === 'touch') {
                // إرسال اللمسة للأندرويد فقط (جميع العملاء عدا المرسل)
                wss.clients.forEach((client) => {
                    if (client !== ws && client.readyState === 1) {
                        client.send(messageString);
                    }
                });
                return;
            }
        } catch (e) {
            // إذا فشل الـ JSON، فهي صورة، أرسلها للجميع
            wss.clients.forEach((client) => {
                if (client !== ws && client.readyState === 1) {
                    client.send(data);
                }
            });
        }
    });
});

const port = process.env.PORT || 3000;
server.listen(port, () => {
    console.log(`Server is running on port ${port}`);
});
