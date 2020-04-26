var express = require("express");
var port = process.env.PORT || 3000;
var app = express();
//https://www.youtube.com/watch?v=HPIjjFGYSJ4
//Initialize the server with node index.js (or) nodejs index.js

const cors = require('cors');
const path = require('path');
const mysql = require('mysql');
const uuidv4 = require("uuid/v4")

app.use(cors());

const SELECT_ALL_USERS = "SELECT * FROM safezone_locals";

var connection;
function attemptConnect() {
    
    connection = mysql.createConnection({
        host: 'HIDDEN', // '',
        user: 'HIDDEN', //'',
        password: 'HIDDEN',
        database: 'HIDDEN' //''
    });

    connection.connect(err => {
        if (err) {
            console.log("❌ Error with the initial connection, trying again in 2 seconds. ", err);
            setTimeout(attemptConnect, 2000);
            //throw err; 
        }
        console.log("📡 Connected without error.");
    });
    
    connection.on('error', function(err) {
        console.log('Server ~ encountered an error while connecting to database ', err);
        if(err.code === 'PROTOCOL_CONNECTION_LOST') {
            console.log("⚠️Connection was lost. Reconnecting.");
            attemptConnect();                         
        } else {     
            console.log("❌ Some other error occured!");
            throw err;                                  
        }
    });
}

attemptConnect();

// console.log(connection);

app.get('/', (req, res) => {
    res.send('Nothing here to see! Did you mean /users?');
});


app.get('/users', (req, res) => {
    connection.query(SELECT_ALL_USERS, (err, results) => {
        if (err) return res.send(err)
        else {
            return res.send(results)
        }
    });
});

app.get('/users/exists', (req, res) => {
    const { d_name} = req.query;
    const QUERY = `SELECT 1 FROM safezone_locals WHERE email = "'${d_name}'"`;
    connection.query(QUERY, (err, results) => {
        if (err) {
            console.log(err); return res.send(err); //inform the user
        } else {
            console.log(results);
            return res.json({
                data: results
            });
        }
    });
});

app.get('/users/byEmail', (req, res) => {
    const { d_name} = req.query;
    const QUERY = `SELECT * FROM safezone_locals WHERE email = '${d_name}' LIMIT 2`;
    connection.query(QUERY, (err, results) => {
        if (err) {
            console.log(err); return res.send(err); //inform the user
        } else {
            console.log("Email query had result " + results + ", for " + d_name + " with query=" + QUERY);
            res.send(results);
        }
    });
});

const { exec } = require('child_process');
// use /execute/mlpreg?array=a,b,c
app.get('/execute/mlpreg', (req, res) => {
    var arr = req.query.array.split(',');
    
    var builder = '';
    for (let val in arr) {
        builder = builder + arr[val] + ' ';
    }
    
    var cmd = 'node MLPRegressor.js ' + builder;
    console.log('> ' + cmd);
    
    exec(cmd, (err, stdout, stderr) => {
        if (err) {
            // node couldn't execute the command
            return;
        }

        // the *entire* stdout and stderr (buffered)
        console.log(`stdout: ${stdout}`);
        console.log(`stderr: ${stderr}`);
        res.send(`${stdout}`);
    });

});


// but you can add parameters through /device/add?d_name=coolname
// With multiple parameters, just do  /device/add?d_name=coolname&some=10&others=F
//INSERT INTO `safezone_locals` (`id`, `email`, `phone`) VALUES ('80213424', 'a@b.c', '8524695842');
app.get('/users/add', (req, res) => {
    const { d_name, d_phone, d_healthy, d_zone/*, some, others */} = req.query;
    d_id = uuidv4()
//new Date().getTime();
    
    const INSERT_DEVICE_QUERY = `INSERT INTO safezone_locals (id, email, phone, healthy, zone) VALUES ('${d_id}', '${d_name}', '${d_phone}', '${d_healthy}', '${d_zone}')` //note: use '${d_name}' for strings, use ${no_apostrophe} for ints, and so on.
    
    console.log("Request add user with query " + INSERT_DEVICE_QUERY + " ⌛ ");
    
    connection.query(INSERT_DEVICE_QUERY, (err, results) => {
        if (err) {
            //throw err will crash the server, we don't want that in real life!
            console.log(err); //log error on the server
            return res.send(err); //inform the user
        } else {
            console.log("Success add user email=" + d_name + " 🗸 ");
            return res.send("Added user email=" + d_name + ".");
        }
    });
    
});


app.listen(port, function () {
 console.log("Example app listening on port!");
});

