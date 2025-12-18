from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

class Item(BaseModel):
    name: str
    price: float

@app.get('/')
def read_root():
    return {"message": "Hello DevPro!"}

@app.post('/item')
def create_item(item: Item):
    return {"item": item.name, "price": item.price}