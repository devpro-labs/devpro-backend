import express from "express";

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

let users = [];
let i = 1;

// Email validator
function isValidEmail(email) {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
}

// Home route
app.get("/", (req, res) => {
  res.json({ message: "Hello Express" });
});

// GET all users
app.get("/users", (req, res) => {
  return res.json(users);
});

// GET user by ID
app.get("/users/:id", (req, res) => {
  const id = parseInt(req.params.id);

  const user = users.find(u => u.id === id);

  if (!user) {
    return res.status(404).json({ error: "User not found" });
  }

  return res.status(200).json({
    email: user.email
  });
});

// CREATE user
app.post("/users", (req, res) => {
  const { name, email, age } = req.body;

  // email validation
  if (!isValidEmail(email)) {
    return res.status(400).json({
      error: "Invalid email format"
    });
  }

  const newUser = {
    id: i++,
    name,
    email,
    age
  };

  console.log("user is created ", newUser)

  users.push(newUser);
  
  return res.status(201).json({
    ...req.body, 
    message: "User created successfully"
  });
});

// UPDATE user age
app.put("/users/:id", (req, res) => {
  const id = parseInt(req.params.id);
  const { age } = req.body;

  const user = users.find(u => u.id === id);

  if (!user) {
    return res.status(404).json({
      error: "User not found"
    });
  }

  user.age = age;

  return res.status(200).json({
    age: user.age,
    message: "User updated successfully"
  });
});

// DELETE user
app.delete("/users/:id", (req, res) => {
  const id = parseInt(req.params.id);

  const index = users.findIndex(u => u.id === id);

  if (index === -1) {
    return res.status(404).json({
      error: "User not found"
    });
  }

  users.splice(index, 1);

  return res.status(200).json({
    message: "User deleted successfully"
  });
});

app.listen(3000, () => {
  console.log("Server running on port 3000 http://localhost:3000");
});
