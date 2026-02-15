import express from "express";

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

let users = [];
let i = 1;
app.get("/", (req, res) => {
  console.log("hey")
  res.json({ message: "Hello Express" });
});
app.get("/users", (req, res) => {
  res.json(users);
});

app.post("/users", (req,res) => {
  const { user } = req.body;
  const obj = {
    ...user,
    id: i,
    message: "User created successfully"
  }
  i++;
  users.push(obj)
  return res.json(obj);
})

app.get("/users/:id", (req, res) => {
  const id = req.params.id;

  let user = null;
  for (let u in users) {
    if (u.id == id) {
      return res.json({
        email : u.email
      })
    }
  }

  return res.json({
    message : "user not found"
  })
})

// app.put("/users/:id", (req, res) => {
//   const id = req.params.id;
//   const {age} = req.json();

//   let user = null;
//   for (let u in users) {
//     if (u.id == id) {
      
//       return res.json({
//         email: u.email
//       })
//     }
//   }

//   return res.json({
//     message: "user not found"
//   })
// })

app.listen(3000, () => {
  console.log("Server running on port 3000");
});