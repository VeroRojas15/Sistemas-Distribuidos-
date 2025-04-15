from flask import Flask, request, jsonify
import requests

app = Flask(__name__)

@app.route('/convert')
def convert():
    value = request.args.get('value')
    to = request.args.get('to')

    if not value or not to:
        return jsonify({'error': 'Parámetros requeridos: value y to'}), 400

    try:
        if to.lower() == 'celsius':
            response = requests.get(f'http://localhost:5001/convert?value={value}')
        elif to.lower() == 'fahrenheit':
            response = requests.get(f'http://localhost:5002/convert?value={value}')
        else:
            return jsonify({'error': 'Parámetro "to" inválido. Usa "celsius" o "fahrenheit"'}), 400

        return jsonify(response.json())

    except requests.exceptions.RequestException as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(port=5000)
