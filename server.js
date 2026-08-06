const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const path = require('path');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

app.use(express.static(path.join(__dirname, '../'))); // الوصول للملفات في المجلد الرئيسي

app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, '../index.html'));
});

wss.on('connection', (ws) => {
    console.log('New connection established');
    ws.on('message', (data) => {
        // إذا كانت البيانات نصية (JSON)
        if (typeof data === 'string' || data instanceof Buffer) {
            try {
                const message = data.toString();
                if (message.startsWith('{')) {
                    const json = JSON.parse(message);
                    
                    // إعادة توجيه فريمات الكاميرا واللمس والنصوص للجميع
                    wss.clients.forEach((client) => {
                        if (client !== ws && client.readyState === WebSocket.OPEN) {
                            client.send(message);
                        }
                    });
                    return;
                }
            } catch (e) {}
        }

        // توجيه البيانات الثنائية (صور الشاشة) للجميع
        wss.clients.forEach((client) => {
            if (client !== ws && client.readyState === WebSocket.OPEN) {
                client.send(data);
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => console.log('Server is live!'));
