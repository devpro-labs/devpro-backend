import express, { Request, Response } from "express";
import { Pool } from "pg";
import dotenv from "dotenv";

dotenv.config();

const app = express();
app.use(express.json());

// 🔹 ENV config
const pool = new Pool({
  host: process.env.PG_HOST,
  user: process.env.PG_USER,
  password: process.env.PG_PASSWORD,
  database: process.env.PG_DATABASE,
  port: Number(process.env.PG_PORT) || 5432,
});

// 🔹 Create table (auto)
const initDB = async () => {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS products (
      qid INTEGER PRIMARY KEY,
      sku VARCHAR(255) UNIQUE NOT NULL,
      name VARCHAR(255) NOT NULL,
      stock INTEGER DEFAULT 0
    );
  `);
};

initDB();

// ✅ POST /products
app.post("/products", async (req: Request, res: Response) => {
  try {
    const { qid, sku, name, stock } = req.body;

    // 🔹 Validation
    if (!qid || !sku || !name) {
      return res.status(400).json({ error: "qid, sku, and name are required" });
    }

    const result = await pool.query(
      "INSERT INTO products (qid, sku, name, stock) VALUES ($1, $2, $3, $4) RETURNING *",
      [qid, sku, name, stock ?? 0]
    );

    res.status(201).json(result.rows[0]);
  } catch (err: any) {
    if (err.code === "23505") {
      return res.status(400).json({ error: "Duplicate qid or SKU" });
    }
    res.status(500).json({ error: "Server error" });
  }
});

// ✅ GET /products/:qid
app.get("/products/:qid", async (req: Request, res: Response) => {
  const { qid } = req.params;

  const result = await pool.query(
    "SELECT * FROM products WHERE qid = $1",
    [qid]
  );

  if (result.rows.length === 0) {
    return res.status(404).json({ error: "Product not found" });
  }

  res.json({
    "sku": result.rows[0].sku,
    "name": result.rows[0].name,
    "stock": result.rows[0].stock
  });
});

// ✅ PATCH /products/:qid/stock
app.patch("/products/:qid/stock", async (req: Request, res: Response) => {
  const { qid } = req.params;
  const { stock } = req.body;

  if (stock === undefined) {
    return res.status(400).json({ error: "stock is required" });
  }

  const result = await pool.query(
    "UPDATE products SET stock = $1 WHERE qid = $2 RETURNING *",
    [stock, qid]
  );

  if (result.rows.length === 0) {
    return res.status(404).json({ error: "Product not found" });
  }

  res.json({
    "stock": stock
  });
});

// ✅ GET /products/low-stock/:threshold
app.get("/products/low-stock/:threshold", async (req: Request, res: Response) => {
  const { threshold } = req.params;

  const result = await pool.query(
    "SELECT * FROM products WHERE stock <= $1",
    [threshold]
  );

  res.json({
    count: result.rows.length
  });
});

// 🔥 Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🚀 Server running on port ${PORT}`);
});