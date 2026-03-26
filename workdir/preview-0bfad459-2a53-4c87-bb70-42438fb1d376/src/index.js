import express from "express";
import {pool} from "./pg.js"

const app = express();
app.use(express.json());

app.get("/", (req, res) => {
  res.json({ message: "Hello Express" });
});

app.listen(3000, () => {
  pool.connect()
    .then(() => console.log("✅ PostgreSQL connected"))
    .catch(err => console.error("❌ Connection error", err));
  console.log("Server running on port 3000");
});