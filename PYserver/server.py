from selenium import webdriver
from http.server import HTTPServer, BaseHTTPRequestHandler
from selenium.webdriver import ActionChains
from selenium.webdriver.common.keys import Keys
import time
import json
import re

from selenium.webdriver.support.select import Select

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


        response = scrape(date, reg2, city)

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