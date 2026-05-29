const express = require('express');
const http = require('http');
const { WebSocketServer } = require('ws');
const path = require('path');

const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server });

// إضافة هذا السطر مهم جداً ليعرف السيرفر مكان الملفات الثابتة (مثل index.html)
app.use(express.static(__dirname)); 

app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

wss.on('connection', (ws) => {
    ws.on('message', (data) => {
        try {
            const messageString = data.toString();
            // تأكد أن البيانات قابلة للتحويل إلى JSON
            const parsed = JSON.parse(messageString);
            
            // إعادة توجيه اللمس والنصوص
            if (parsed.type === 'touch' || parsed.type === 'text') {
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
