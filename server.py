# Attivare Ambiente Virtuale: source .venv/bin/activate

from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.orm import relationship
from sqlalchemy import ForeignKey, Column, String, Float, Integer

app = Flask(__name__)

# Configurazione del database (SQLite)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///arrangement_manager.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)

# ------------------------------------------------------
# MODELLI DEL DATABASE ---> @Entity di kotlin

class User(db.Model):
    __tablename__ = 'User'
    email = db.Column(db.String, primary_key=True)
    password = db.Column(db.String, nullable=False)

    tables = relationship("Table", backref="user", lazy=True, cascade="all, delete-orphan")
    menu_items = relationship("MenuItem", backref="user", lazy=True, cascade="all, delete-orphan")
    order_entries = relationship("OrderEntry", backref="user", lazy=True, cascade="all, delete-orphan")

    def to_dict(self):
        """Metodo per serializzare l'oggetto User in un dizionario."""
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
        """Metodo per serializzare l'oggetto Table in un dizionario."""
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
    description = db.Column(db.String, nullable=False)

    def to_dict(self):
        """Metodo per serializzare l'oggetto MenuItem in un dizionario."""
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
        """Metodo per serializzare l'oggetto OrderEntry in un dizionario."""
        return {
            'table_name': self.table_name,
            'menu_item_name': self.menu_item_name,
            'id_user': self.id_user,
            'quantity': self.quantity
        }

    def to_dict(self):
        """Metodo per serializzare l'oggetto OrderEntry in un dizionario."""
        return {
            'table_name': self.table_name,
            'menu_item_name': self.menu_item_name,
            'id_user': self.id_user,
            'quantity': self.quantity
        }

# ------------------------------------------------------
# ENDPOINT API ---> Funzioni DAO

# Gestione Utenti
@app.route('/users/register', methods=['POST'])
def register_user():
    """Endpoint per registrare un nuovo utente (= insertUser)"""
    data = request.json
    email = data.get('email')
    password = data.get('password')

    if not email or not password:
        return jsonify({"error": "Email e password sono richiesti"}), 400

    if User.query.get(email):
        return jsonify({"error": "L'email esiste già"}), 409

    new_user = User(email=email, password=password)
    db.session.add(new_user)
    db.session.commit()
    return jsonify(new_user.to_dict()), 201

@app.route('/users/login', methods=['POST'])
def login_user():
    """Endpoint per il login (= getUserByEmail, emailExist)"""
    data = request.json
    email = data.get('email')
    password = data.get('password')

    user = User.query.get(email)
    if user and user.password == password:
        return jsonify({"message": "Login avvenuto con successo", "user": user.to_dict()}), 200
    return jsonify({"error": "Credenziali non valide"}), 401

# --- Gestione Tavoli ---
@app.route('/users/<string:userId>/tables', methods=['GET'])
def get_all_tables_by_user(userId):
    """Endpoint per recuperare tutti i tavoli di un utente (= getAllTablesByUsers)"""
    tables = Table.query.filter_by(id_user=userId).order_by(Table.name).all()
    return jsonify([table.to_dict() for table in tables]), 200

@app.route('/users/<string:userId>/tables', methods=['POST'])
def insert_table(userId):
    """Endpoint per inserire un nuovo tavolo (= insertTable)."""
    data = request.json
    table_name = data.get('name')
    x_coordinate = data.get('x_coordinate')
    y_coordinate = data.get('y_coordinate')
    width = data.get('width')
    height = data.get('height')

    if not all([table_name, x_coordinate, y_coordinate, width, height]):
        return jsonify({"error": "Dati del tavolo incompleti"}), 400

    # Verifica se l'utente esiste
    if not User.query.get(userId):
        return jsonify({"error": "Utente non trovato"}), 404

    # Controlla se il tavolo esiste già
    if Table.query.get((table_name, userId)):
        return jsonify({"error": "Il tavolo esiste già per questo utente"}), 409

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
    """Endpoint per aggiornare un tavolo esistente (= updateTable)."""
    table = Table.query.get((name, userId))
    if not table:
        return jsonify({"error": "Tavolo non trovato"}), 404

    data = request.json
    table.x_coordinate = data.get('x_coordinate', table.x_coordinate)
    table.y_coordinate = data.get('y_coordinate', table.y_coordinate)
    table.width = data.get('width', table.width)
    table.height = data.get('height', table.height)

    db.session.commit()
    return jsonify(table.to_dict()), 200

@app.route('/users/<string:userId>/tables/<string:name>', methods=['DELETE'])
def delete_table(userId, name):
    """Endpoint per eliminare un tavolo (= deleteTableByNameAndUser)."""
    table = Table.query.get((name, userId))
    if not table:
        return jsonify({"error": "Tavolo non trovato"}), 404

    db.session.delete(table)
    db.session.commit()
    return jsonify({"message": "Tavolo eliminato con successo"}), 200

# --- Gestione Menu ---
@app.route('/users/<string:userId>/menu', methods=['GET'])
def get_all_menu_by_user(userId):
    """Endpoint per recuperare tutti gli elementi del menu di un utente (= getAllMenuByUser)."""
    menu_items = MenuItem.query.filter_by(id_user=userId).order_by(MenuItem.name).all()
    return jsonify([item.to_dict() for item in menu_items]), 200

@app.route('/users/<string:userId>/menu', methods=['POST'])
def insert_menu_item(userId):
    """Endpoint per inserire un elemento nel menu (= insertMenu)."""
    data = request.json
    name = data.get('name')
    price = data.get('price')
    quantity = data.get('quantity')
    description = data.get('description')

    if not all([name, price is not None, quantity is not None, description]):
        return jsonify({"error": "Dati dell'elemento menu incompleti"}), 400

    # Verifica se l'utente esiste
    if not User.query.get(userId):
        return jsonify({"error": "Utente non trovato"}), 404

    # Controlla se l'elemento del menu esiste già
    if MenuItem.query.get((name, userId)):
        return jsonify({"error": "L'elemento del menu esiste già per questo utente"}), 409

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
    """Endpoint per aggiornare un elemento del menu (= updateMenuItem)."""
    menu_item = MenuItem.query.get((name, userId))
    if not menu_item:
        return jsonify({"error": "Elemento del menu non trovato"}), 404

    data = request.json
    menu_item.price = data.get('price', menu_item.price)
    menu_item.quantity = data.get('quantity', menu_item.quantity)
    menu_item.description = data.get('description', menu_item.description)

    db.session.commit()
    return jsonify(menu_item.to_dict()), 200

@app.route('/users/<string:userId>/menu/<string:name>', methods=['DELETE'])
def delete_menu_item(userId, name):
    """Endpoint per eliminare un elemento del menu (= deleteMenuItemByNameAndUser)."""
    menu_item = MenuItem.query.get((name, userId))
    if not menu_item:
        return jsonify({"error": "Elemento del menu non trovato"}), 404

    db.session.delete(menu_item)
    db.session.commit()
    return jsonify({"message": "Elemento del menu eliminato con successo"}), 200

# --- Gestione Ordini ---
@app.route('/users/<string:userId>/orders', methods=['POST'])
def insert_order_entries(userId):
    """Endpoint per inserire una lista di voci d'ordine (= insertOrderEntries)."""
    order_entries_data = request.json
    if not isinstance(order_entries_data, list):
        return jsonify({"error": "Il corpo della richiesta deve essere una lista di ordini"}), 400
    
    new_entries = []
    for entry_data in order_entries_data:
        table_name = entry_data.get('table_name')
        menu_item_name = entry_data.get('menu_item_name')
        quantity = entry_data.get('quantity')
        
        if not all([table_name, menu_item_name, quantity is not None]):
            return jsonify({"error": "Dati dell'ordine incompleti"}), 400

        # Verifica che il tavolo e l'elemento del menu esistano per l'utente
        if not Table.query.get((table_name, userId)):
            return jsonify({"error": f"Tavolo '{table_name}' non trovato per l'utente '{userId}'"}), 404
        if not MenuItem.query.get((menu_item_name, userId)):
            return jsonify({"error": f"Elemento menu '{menu_item_name}' non trovato per l'utente '{userId}'"}), 404

        new_entry = OrderEntry(
            table_name=table_name,
            menu_item_name=menu_item_name,
            id_user=userId,
            quantity=quantity
        )
        new_entries.append(new_entry)

    db.session.bulk_save_objects(new_entries)
    db.session.commit()
    return jsonify([entry.to_dict() for entry in new_entries]), 201


if __name__ == '__main__':
    # Creazione delle tabelle del database se non esistono
    with app.app_context():
        db.create_all()
    
    # Esegue l'app in modalità debug. In produzione, usa un server WSGI.
    app.run(debug=True)
