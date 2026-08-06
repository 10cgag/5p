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
            const messageString = data.toString();
            const parsed = JSON.parse(messageString);
            
            // إعادة توجيه اللمس والنصوص وجميع أوامر الكيبورد (المسح والـ Enter)
            if (parsed.type === 'touch' || parsed.type === 'text' || parsed.type === 'key') {
                wss.clients.forEach((client) => {
                    if (client !== ws && client.readyState === 1) {
                        client.send(messageString);
                    }
                });
            }
        } catch (e) {
            // إعادة توجيه الصور (Binary)
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
