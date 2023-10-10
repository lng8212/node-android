const express = require('express');
const bodyParser = require('body-parser');

const app = express();
const port = 3000;

// Middleware to parse JSON request bodies
app.use(bodyParser.json());

// Define a route to receive data from Android client
app.post('/api/vol_data', (req, res) => {
  const dataFromClient = req.body;

  console.log('Received data from Android client:', dataFromClient);
  res.status(200).json({ message: 'Data received successfully' });
});


// Start the server
app.listen(port, () => {
  console.log(`Server is running on port ${port}`);
});