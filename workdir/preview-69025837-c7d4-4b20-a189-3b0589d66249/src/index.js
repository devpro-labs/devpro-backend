import express from "express";
import dotenv from "dotenv";
import { connectDB } from "./db.js";
import { User } from "./user.model.js";

dotenv.config();

const app = express();
app.use(express.json());

// 🔹 Health check
app.get("/health", (req, res) => {
  res.json({ message: "server is healthy" });
});

// 🔹 Create user
app.post("/users", async (req, res) => {
  try {
    const { name } = req.body;

    if (!name) {
      return res.status(400).json({ error: "Name is required" });
    }

    const user = await User.create({ name });

    return res.status(201).json({
      name: user.name
    });

  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

// 🔹 Get user by ID
app.get("/users/:id", async (req, res) => {
  try {
    const { id } = req.params;

    const user = await User.findById(id);

    if (!user) {
      return res.status(404).json({ error: "User not found" });
    }

    return res.json({
      name: user.name
    });

  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

// 🔹 Start server
const PORT = process.env.PORT || 3000;

app.listen(PORT, async () => {
  await connectDB();
  console.log(`🚀 Server running on port ${PORT}`);
});