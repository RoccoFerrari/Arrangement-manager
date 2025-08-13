# Activate Virtual Environment: source venv/bin/activate
# Compile: python3 server.py

import os
from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.orm import relationship
from sqlalchemy import ForeignKey, Column, String, Float, Integer

app = Flask(__name__)

basedir = os.path.abspath(os.path.dirname(__file__))

# Database configuration (SQLite)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///' + os.path.join(basedir, 'arrangement_manager.db')
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)

# ------------------------------------------------------
# DATABASE MODELS ---> Kotlin's @Entity

class User(db.Model):
    __tablename__ = 'User'
    email = db.Column(db.String, primary_key=True)
    password = db.Column(db.String, nullable=False)

    tables = relationship("Table", backref="user", lazy=True, cascade="all, delete-orphan")
    menu_items = relationship("MenuItem", backref="user", lazy=True, cascade="all, delete-orphan")
    order_entries = relationship("OrderEntry", backref="user", lazy=True, cascade="all, delete-orphan")

    def to_dict(self):
        """Method to serialize the User object into a dictionary"""
        return {
            'email': self.email,
            'password': self.password
        }

class Table(db.Model):
    __tablename__ = 'Table_'
    name = db.Column(db.String, primary_key=True)
    id_user = db.Column(db.String, db.ForeignKey('User.email', ondelete='CASCADE'), primary_key=True)
    x_coordinate = db.Column(db.Float, nullable=False)
    y_coordinate = db.Column(db.Float, nullable=False)
    width = db.Column(db.Float, nullable=False)
    height = db.Column(db.Float, nullable=False)

    def to_dict(self):
        """Method to serialize the Table object into a dictionary"""
        return {
            'name': self.name,
            'x_coordinate': self.x_coordinate,
            'y_coordinate': self.y_coordinate,
            'width': self.width,
            'height': self.height,
            'id_user': self.id_user
        }

class MenuItem(db.Model):
    __tablename__ = 'Menu'
    name = db.Column(db.String, primary_key=True)
    id_user = db.Column(db.String, db.ForeignKey('User.email', ondelete='CASCADE'), primary_key=True)
    price = db.Column(db.Float, nullable=False)
    quantity = db.Column(db.Integer, nullable=False)
    description = db.Column(db.String)

    def to_dict(self):
        """Method to serialize the MenuItem object into a dictionary"""
        return {
            'name': self.name,
            'price': self.price,
            'quantity': self.quantity,
            'description': self.description,
            'id_user': self.id_user
        }

class OrderEntry(db.Model):
    __tablename__ = 'OrderEntry'
    table_name = db.Column(db.String, primary_key=True)
    menu_item_name = db.Column(db.String, primary_key=True)
    id_user = db.Column(db.String, db.ForeignKey('User.email', ondelete='CASCADE'), primary_key=True)
    quantity = db.Column(db.Integer, nullable=False)

    __table_args__ = (
        db.ForeignKeyConstraint(
            ['table_name', 'id_user'],
            ['Table_.name', 'Table_.id_user'],
            ondelete='CASCADE'
        ),
        db.ForeignKeyConstraint(
            ['menu_item_name', 'id_user'],
            ['Menu.name', 'Menu.id_user'],
            ondelete='CASCADE'
        ),
    )

    def to_dict(self):
        """Method to serialize the OrderEntry object into a dictionary"""
        return {
            'table_name': self.table_name,
            'menu_item_name': self.menu_item_name,
            'id_user': self.id_user,
            'quantity': self.quantity
        }

    def to_dict(self):
        """Method to serialize the OrderEntry object into a dictionary"""
        return {
            'table_name': self.table_name,
            'menu_item_name': self.menu_item_name,
            'id_user': self.id_user,
            'quantity': self.quantity
        }

# ------------------------------------------------------
# ENDPOINT API ---> DAO Functions

# User management
@app.route('/users/register', methods=['POST'])
def register_user():
    """Endpoint to register a new user (= insertUser)"""
    data = request.json
    email = data.get('email')
    password = data.get('password')

    if not email or not password:
        return jsonify({"error": "Email and password are required"}), 400

    if User.query.get(email):
        return jsonify({"error": "The email already exists"}), 409

    new_user = User(email=email, password=password)
    db.session.add(new_user)
    db.session.commit()
    return jsonify(new_user.to_dict()), 201

@app.route('/users/login', methods=['POST'])
def login_user():
    """Endpoint for login"""
    data = request.json
    email = data.get('email')
    password = data.get('password')

    user = User.query.get(email)

    # The user does not exist in the database
    if not user:
        return jsonify({"error": "User not found"}), 404

    # The user exists, let's check the password
    if user.password == password:
        return jsonify({"message": "Login successful", "user": user.to_dict()}), 200
    else:
        # The user exists but the password is incorrect
        return jsonify({"error": "Credenziali non valide"}), 401

# --- Table management ---
@app.route('/users/<string:userId>/tables', methods=['GET'])
def get_all_tables_by_user(userId):
    """Endpoint to retrieve all tables of a user (= getAllTablesByUsers)"""
    tables = Table.query.filter_by(id_user=userId).order_by(Table.name).all()
    return jsonify([table.to_dict() for table in tables]), 200

@app.route('/users/<string:userId>/tables', methods=['POST'])
def insert_table(userId):
    """Endpoint to insert a new table (= insertTable)"""
    data = request.json
    table_name = data.get('name')
    x_coordinate = data.get('x_coordinate')
    y_coordinate = data.get('y_coordinate')
    width = data.get('width')
    height = data.get('height')

    if not all([table_name, x_coordinate, y_coordinate, width, height]):
        return jsonify({"error": "Incomplete table data"}), 400

    # Check if the user exists
    if not User.query.get(userId):
        return jsonify({"error": "User not found"}), 404

    # Check if the table already exists
    if Table.query.get((table_name, userId)):
        return jsonify({"error": "The table already exists for this user"}), 409

    new_table = Table(
        name=table_name,
        id_user=userId,
        x_coordinate=x_coordinate,
        y_coordinate=y_coordinate,
        width=width,
        height=height
    )
    db.session.add(new_table)
    db.session.commit()
    return jsonify(new_table.to_dict()), 201

@app.route('/users/<string:userId>/tables/<string:name>', methods=['PUT'])
def update_table(userId, name):
    """Endpoint to update an existing table (= updateTable)"""
    table = Table.query.get((name, userId))
    if not table:
        return jsonify({"error": "Table not found"}), 404

    data = request.json
    table.x_coordinate = data.get('x_coordinate', table.x_coordinate)
    table.y_coordinate = data.get('y_coordinate', table.y_coordinate)
    table.width = data.get('width', table.width)
    table.height = data.get('height', table.height)

    db.session.commit()
    return jsonify(table.to_dict()), 200

@app.route('/users/<string:userId>/tables/<string:name>', methods=['DELETE'])
def delete_table(userId, name):
    """Endpoint to delete a table (= deleteTableByNameAndUser)"""
    table = Table.query.get((name, userId))
    if not table:
        return jsonify({"error": "Table not found"}), 404

    db.session.delete(table)
    db.session.commit()
    return jsonify({"message": "Table successfully cleared"}), 200

# --- Menu management ---
@app.route('/users/<string:userId>/menu', methods=['GET'])
def get_all_menu_by_user(userId):
    """Endpoint to retrieve all menu items for a user (= getAllMenuByUser)"""
    menu_items = MenuItem.query.filter_by(id_user=userId).order_by(MenuItem.name).all()
    return jsonify([item.to_dict() for item in menu_items]), 200

@app.route('/users/<string:userId>/menu', methods=['POST'])
def insert_menu_item(userId):
    """Endpoint to insert an item into the menu. If it exists, it replaces it"""
    data = request.json
    name = data.get('name')
    price = data.get('price')
    quantity = data.get('quantity')
    description = data.get('description')

    if not all([name, price is not None, quantity is not None]):
        return jsonify({"error": "Incomplete menu item data"}), 400

    # Check if the user exists
    if not User.query.get(userId):
        return jsonify({"error": "User not found"}), 404

    # Check if the menu item already exists
    existing_item = MenuItem.query.get((name, userId))

    if existing_item:
        # Check if the item exists, update it with the new data if the item already exists
        existing_item.price = price
        existing_item.quantity = quantity
        existing_item.description = description
        db.session.commit()
        return jsonify(existing_item.to_dict()), 200
    else:
        # If it doesn't exist, create a new itemIf it doesn't exist, create a new item
        new_item = MenuItem(
            name=name,
            id_user=userId,
            price=price,
            quantity=quantity,
            description=description
        )
        db.session.add(new_item)
        db.session.commit()
        return jsonify(new_item.to_dict()), 201

@app.route('/users/<string:userId>/menu/<string:name>', methods=['PUT'])
def update_menu_item(userId, name):
    """Endpoint to update a menu item (= updateMenuItem)"""
    menu_item = MenuItem.query.get((name, userId))
    if not menu_item:
        return jsonify({"error": "Menu item not found"}), 404

    data = request.json
    menu_item.price = data.get('price', menu_item.price)
    menu_item.quantity = data.get('quantity', menu_item.quantity)
    menu_item.description = data.get('description', menu_item.description)

    db.session.commit()
    return jsonify(menu_item.to_dict()), 200

# --- Order management ---
@app.route('/users/<string:userId>/orders', methods=['POST'])
def insert_order_entries(userId):
    """Endpoint to insert or update order items."""
    order_entries_data = request.json
    if not isinstance(order_entries_data, list):
        return jsonify({"error": "The body of the request must be a list of orders"}), 400
    
    for entry_data in order_entries_data:
        table_name = entry_data.get('table_name')
        menu_item_name = entry_data.get('menu_item_name')
        quantity = entry_data.get('quantity')
        
        if not all([table_name, menu_item_name, quantity is not None]):
            return jsonify({"error": "Incomplete order data"}), 400

        if not Table.query.get((table_name, userId)):
            return jsonify({"error": f"Table '{table_name}' not found for user '{userId}'"}), 404
        if not MenuItem.query.get((menu_item_name, userId)):
            return jsonify({"error": f"Menu item '{menu_item_name}' not found for user '{userId}'"}), 404

        # Check if there is already an order for this table, dish and user
        existing_order = OrderEntry.query.get((table_name, menu_item_name, userId))
        
        if existing_order:
            # If the order exists, update the quantity
            existing_order.quantity += quantity
        else:
            # If it does not exist, create a new order
            new_entry = OrderEntry(
                table_name=table_name,
                menu_item_name=menu_item_name,
                id_user=userId,
                quantity=quantity
            )
            db.session.add(new_entry)
            
    db.session.commit()
    
    updated_entries = OrderEntry.query.filter_by(id_user=userId, table_name=table_name).all()
    return jsonify([entry.to_dict() for entry in updated_entries]), 201

if __name__ == '__main__':
    # Creating database tables if they do not exist
    with app.app_context():
        db.create_all()
    
    # host='0.0.0.0' accept connections from any device on the network
    app.run(host='0.0.0.0', debug=True, port=5000)
