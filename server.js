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
        try {
            // محاولة تحويل البيانات إلى نص (JSON)
            const messageString = data.toString();
            const parsed = JSON.parse(messageString);
            
            // تحقق: هل هي رسالة نصية أم أمر لمس؟
            if (parsed.type === 'text' || parsed.type === 'delete' || parsed.type === 'touch') {
                // إرسال البيانات المحددة فقط للعملاء الآخرين
                wss.clients.forEach((client) => {
                    if (client !== ws && client.readyState === 1) {
                        client.send(messageString);
                    }
                });
            }
        } catch (e) {
            // إذا كانت البيانات صورة (Binary)، يتم إرسالها كما هي
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
