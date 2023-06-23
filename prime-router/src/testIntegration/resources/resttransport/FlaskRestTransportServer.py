from flask import Flask, jsonify, request

app = Flask(__name__)


@app.route("/auth", methods=["POST"])
def auth():
    return jsonify({"access_token": "accessToken", "expires_in": 3600})


@app.route("/report", methods=["POST"])
def report():
    return jsonify({
        "status": "Success",
        "received": {
            "headers": dict(request.headers),
            "body": request.json
        }
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=80, debug=True)
