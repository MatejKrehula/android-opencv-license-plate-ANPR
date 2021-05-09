from selenium import webdriver
from http.server import HTTPServer, BaseHTTPRequestHandler
from selenium.webdriver import ActionChains
from selenium.webdriver.common.keys import Keys
import time
import json
import re
import psycopg2 as psycopg2
from datetime import datetime, timedelta
from selenium.webdriver.support.select import Select


def addToDatabase(regData, reg):
    conn = None
    try:
        conn = psycopg2.connect(
            host="localhost",
            database="ZavrsniRadBaza",
            user="postgres",
            password="bazepodataka")

        data = json.loads(regData)
        marka = data["marka"]
        sasija = data["sasija"]
        osiguranje = data["osiguranje"]
        polica = data["polica"]
        registered = True
        lastChecked = datetime.now()


        cur = conn.cursor("insert into registrationdata(registration, registered,lastcheckdate, cartype, vinnumber, insurance, insurancepolicy) values (%s,%s,%s,%s,%s,%s,%s)" %(
                "'" + reg + "'", registered,
                "'" + lastChecked + "'",
                "'" + marka + "'",
                "'" + sasija + "'",
                "'" + osiguranje + "'",
                "'" + polica + "'"
        ))
        cur.execute()
        conn.commit()

        cur.close()
    except (Exception, psycopg2.DatabaseError) as error:
        print(error)
    finally:
        if conn is not None:
            conn.close()

def deleteData(registrationData):
    conn = None
    try:
        conn = psycopg2.connect(
            host="localhost",
            database="ZavrsniRadBaza",
            user="postgres",
            password="bazepodataka")

        cur = conn.cursor()
        cur.execute("DELETE FROM registrationdata WHERE registration = %s" % ("'" + registrationData + "'"))
        conn.commit()

        cur.close()
    except (Exception, psycopg2.DatabaseError) as error:
        print(error)
    finally:
        if conn is not None:
            conn.close()


def checkData(registrationData):
    conn = None
    try:
        conn = psycopg2.connect(
            host="localhost",
            database="ZavrsniRadBaza",
            user="postgres",
            password="bazepodataka")

        cur = conn.cursor()
        cur.execute("SELECT * FROM registrationdata WHERE registration = %s;" %("'"+registrationData+"'"))
        row = cur.fetchone()
        if row != None:
           timePassed = datetime.now() - datetime.strptime(row[2], '%Y-%m-%d')
           if timePassed.days > 30 or row[1] == "false":
               return "NE"
           else:
               try:
                   value = {
                       "marka": row[3],
                       "sasija": row[4],
                       "osiguranje": row[5],
                       "polica": row[6]
                   }
                   regFoundValue = json.dumps(value)
               except:
                   regFoundValue = "NE"
               return regFoundValue
        else:
            return "NE"


        cur.close()
    except (Exception, psycopg2.DatabaseError) as error:
        print(error)
    finally:
        if conn is not None:
            conn.close()



def scrape(date, reg2 , city):
    PATH = "C:\chromedriver.exe"

    driver = webdriver.Chrome(PATH)
    driver.get("https://huo.hr/hr/provjera-osiguranja")

    kuki = driver.find_element_by_link_text("PRIHVAĆAM")
    kuki.click()

    driver.refresh()

    driver.find_element_by_id("datum_osiguranja").click()
    driver.find_element_by_link_text(date).click()

    driver.find_element_by_id("reg2").send_keys(reg2)
    driver.find_element_by_link_text("-").click()
    driver.find_element_by_link_text(city).click()

    # driver.refresh()
    link = driver.find_element_by_id("send")
    link.click()

    time.sleep(4)
    try:
        m = re.search('vozila: .+?(?=broj)', driver.page_source)
        if m:
            markaVozila = m.group(0).replace("vozila:", '').strip()
            print(markaVozila)
        m = re.search('šasije: .+', driver.page_source)
        if m:
            brojSasije= m.group(0)
            sep = '<br>'
            brojSasije = brojSasije.split(sep, 1)[0].replace("šasije:", '').strip()
            print(brojSasije)
        m = re.search('društva: .+?(?=,)', driver.page_source)
        if m:
            osigDrustvo = m.group(0).replace("<b>", '').replace("</b>", '').replace("društva:", '').strip()
            print(osigDrustvo)
        m = re.search('odgovornosti: .+', driver.page_source)
        if m:
            policaOdgovornosti = m.group(0).replace('.', '').replace("<b>", '').replace("</b></p></div>", '').replace("odgovornosti:", '').strip()
            print(policaOdgovornosti)
    except:
        driver.quit()
        return "NE"

    try:
        value = {
            "marka" : markaVozila,
            "sasija" :  brojSasije,
            "osiguranje" : osigDrustvo,
            "polica" : policaOdgovornosti
        }
        regFoundValue = json.dumps(value)
    except:
        regFoundValue = "NE"



    if driver.page_source.find("ne raspolaže") == -1:
        retValue = regFoundValue
    else:
        retValue = "NE"
    driver.quit()
    return retValue



class registrationHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        date = self.headers["date"]
        reg2 = self.headers["reg2"]
        city = self.headers["city"]
        #provjeri da li se nalazi u bazi sa datumom ne starijim od 30 dana
        #ako se nalazi onda i dobar je onda ga vrati ako nije dobar provjeri ga i spremi u bazu

        #check if reg in base not older than 30 days if in the
        response = checkData(city + reg2)
        if response == "NE":
            response = scrape(date, reg2, city)
            deleteData(city + reg2)
            if response != "NE":
                addToDatabase(response, city+reg2)

            #izbrisi stari unutra
            #dodaj novi u bazu
            #dodaj u bazu i kad dodajes izbrisi sve stare entrye sa tom registracijom



        self.send_response(200)
        self.end_headers()
        self.wfile.write(response.encode("utf-8"))

    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write("TEST".encode("utf-8"))

def main():
    PORT = 8000
    server = HTTPServer(('', PORT), registrationHandler)
    server.serve_forever()

if __name__ == '__main__':
    main()