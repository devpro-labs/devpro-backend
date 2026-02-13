import express from "express";

const app = express();
app.use(express.json());

app.get("/", (req, res) => {
  console.log("hey")
  res.json({ message: "Hello Express" });
});

app.listen(3000, () => {
  console.log("Server running on port 3000");
});