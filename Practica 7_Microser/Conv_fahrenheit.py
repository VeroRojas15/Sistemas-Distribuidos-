from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/convert')
def convert():
    value = request.args.get('value', type=float)
    if value is None:
        return jsonify({'error': 'Falta el parámetro "value"'}), 400
    fahrenheit = (value * 9 / 5) + 32
    return jsonify({'original': value, 'converted': fahrenheit, 'unit': 'Fahrenheit'})

if __name__ == '__main__':
    app.run(port=5002)
