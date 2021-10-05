import sys
import time
from urllib.parse import quote

# import db as db
from flask import Flask, render_template, request, session
import json
import urllib.request


app = Flask(__name__)

backend = ""

default_script = "".join(open("./example-script.txt").readlines())

# class Simulation(db.Model):
#     # step number
#     id = db.Column(db.Integer, primary_key=True)
#     name = db.Column(db.String(64), index=True)
#     age = db.Column(db.Integer, index=True)
#     address = db.Column(db.String(256))
#     phone = db.Column(db.String(20))
#     email = db.Column(db.String(120), index=True)


def visualise_dot():
    with urllib.request.urlopen(backend + '/toDOT') as response:
        resp : str = response.read().decode("utf-8")
        return json.loads(resp)


def model_check(bmc : bool, bound : int):
    params = {
        'bmc': quote(str(bmc),  safe=''),
        'bound': quote(str(bound),  safe='')
    }
    with urllib.request.urlopen(backend + '/modelCheck?' + urllib.parse.urlencode(params)) as response:
        resp: str = response.read().decode("utf-8")
        return json.loads(resp)


def set_system(script):
    params = {
        'script': quote(script, safe='')
    }
    with urllib.request.urlopen(backend + '/setSystem?' + urllib.parse.urlencode(params)) as response:
        resp: str = response.read().decode("utf-8")
        return json.loads(resp)


def system_init():
    with urllib.request.urlopen(backend + '/init') as response:
        resp: str = response.read().decode("utf-8")
        return resp


def simulate_next(constraint: str):
    params = {
        'constraint': quote(constraint,  safe='')
    }
    with urllib.request.urlopen(backend + '/simulateNext?' + urllib.parse.urlencode(params)) as response:
        resp: str = response.read().decode("utf-8")
        return resp


def jsonToTableHeader(stateObject):
    jsonObject = json.loads(stateObject)

    header2 = '<tr>\n'
    header2 += "<th></th>\n"

    header3 = '<tr>\n'
    header3 += "<th></th>\n"

    header = '<th>Steps</th>\n'
    for object in jsonObject:
        for attribute, value in object:
            header += "<th>" + attribute + "</th>\n"
            for att, val in value:
                header2 += "<th>" + att + "</th>"
                if "variables" in att:
                    for varName, val in val:
                        header3 += "<th>" + varName + "</th>"

    for object in jsonObject:
        for attribute, value in object:
            header += "<th>" + attribute + "</th>\n"
    header += "</tr>\n"


    return "<tr>" + header + "</tr>" + "<tr>" + header2 + "</tr>" + "<tr>" + header3 + "</tr>"


def jsonToTableRow(stateObject):
    jsonObject = json.loads(stateObject)

    body = '<tbody>\n'
    body += '<tr>\n'
    for object in jsonObject:
        for attribute, value in object:
            body += "<th>" + attribute + "</th>\n"
    body += "</tr>\n"
    body += "</tbody\n>"

    return body


@app.route("/", methods=['POST', 'GET'])
def index():
    print("index")
    error = ''
    mcresponse = []
    simresponse = list()
    siminit = ''
    code = default_script
    mc = True
    sim = False
    visualise = ""
    symbolic = False

    if len(request.form) > 0:
        if 'code' in request.form.keys():
            code = request.form['code']
        if 'update' in request.form.keys() and request.form['update']:
            response = set_system(code)
            if 'error' in response:
                error = response['error']
            else:
                visualise = visualise_dot()
        else:
            visualise = visualise_dot() # cache this
        if 'mc' in request.form.keys() and request.form['mc']:
            if 'bmc' in request.form.keys():
                bmc = request.form['bmc']
            else:
                bmc = False
            if 'bmc-bound' in request.form.keys():
                bound = request.form['bmc-bound']
            else:
                bound = 10
            response = model_check(bmc, bound)
            if "error" in response:
                error = response['error']
            else:
                mcresponse = response['results']
            mc = True
            sim = False
        if 'sim' in request.form.keys() and request.form['sim'] != 'false':
            sim = True
            mc = False
            if 'siminit' in request.form and not request.form['siminit']:
                init = system_init()
            siminit = 'True'
            if "simresponse" in request.form:#) and request.form["simresponse"] != '[]':
                prev = (request.form["simresponse"]).replace("\'{", "{").replace("\']", "]").replace("\'",'"').replace("'",'"')
                simresponse = json.loads(prev)
                constraints = request.form['constraints']
                next = simulate_next(constraints).replace("\'",'"').replace("'",'"')
                next = json.loads(next)
                next.update({ "constraints" : constraints});
                simresponse.append(next)
            else:
                simresponse = [simulate_next(request.form['constraints'])]


    return render_template("index.html",
                           code=code,
                           sim=sim,
                           mc=mc,
                           symbolic=symbolic,
                           visualise=visualise,
                           mcresponse=mcresponse,
                           simresponse=(simresponse),
                           siminit=siminit,
                           error=error,
                           update=False)


if __name__ == "__main__":
    if(len(sys.argv) != 3):
        print("Please specify frontend port and backend URL.")
    else:
        backend = sys.argv[2]
        app.run(debug=True, port=sys.argv[1])
