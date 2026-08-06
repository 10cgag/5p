const express = require('express');
const http = require('http');
const { WebSocketServer } = require('ws');
const path = require('path');

const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server });

app.use(express.static(path.join(__dirname)));

app.get('/', (req, res) => res.sendFile(path.join(__dirname, 'index.html')));

wss.on('connection', (ws) => {
    console.log('New device connected');

    ws.on('message', (data) => {
        try {
            // محاولة معالجة الرسائل النصية (أوامر)
            const messageString = data.toString();
            const parsed = JSON.parse(messageString);
            
            // تمرير أوامر اللمس، الكيبورد، وحالة الكاميرا
            if (['touch', 'text', 'key', 'keyboard_state', 'camera_request'].includes(parsed.type)) {
                wss.clients.forEach((client) => {
                    if (client !== ws && client.readyState === 1) {
                        client.send(messageString);
                    }
                });
            }
        } catch (e) {
            // تمرير البيانات الثنائية (Binary) - صور الشاشة أو صور الكاميرا
            wss.clients.forEach((client) => {
                if (client !== ws && client.readyState === 1) {
                    client.send(data);
                }
            });
        }
    });

    ws.on('close', () => console.log('Device disconnected'));
});

const port = process.env.PORT || 3000;
server.listen(port, () => console.log(`Server is running on port ${port}`));
