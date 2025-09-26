/*
  Finalny Firmware dla ESP32 do sterowania silnikiem DC (Wersja 3.2 - z poprawką kompilacji)
  - Sterownik: DRV8833
  - Komunikacja: Bluetooth Low Energy (BLE)
  - Sterowanie: Chwilowe (Momentary)
  - Nazwa BLE: Unikalna, oparta na adresie MAC
*/

// 1. BIBLIOTEKI
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include "esp_mac.h"

// 2. KONFIGURACJA SPRZĘTOWA
const int AIN1 = D2; // Pin AIN1 sterownika DRV8833
const int AIN2 = D3; // Pin AIN2 sterownika DRV8833

// 3. USTAWIENIA BLE
#define SERVICE_UUID           "0A66A21A-422A-4A97-9AF3-575E67A55C7E"
#define MOTOR_CHARACTERISTIC_UUID "CC67E36C-323A-4E36-A33E-039B3E452285"

// 4. FUNKCJE STEROWANIA SILNIKIEM
void motorStop() {
  digitalWrite(AIN1, LOW);
  digitalWrite(AIN2, LOW);
}

void motorRight() {
  digitalWrite(AIN1, HIGH);
  digitalWrite(AIN2, LOW);
}

void motorLeft() {
  digitalWrite(AIN1, LOW);
  digitalWrite(AIN2, HIGH);
}

// 5. CALLBACKS - OBSŁUGA ZDARZEŃ BLE
class MotorCharacteristicCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        String value = pCharacteristic->getValue();
        if (value.length() > 0) {
            byte command = value[0];
            switch(command) {
              case 0x01: // W prawo
                motorRight();
                break;
              case 0x02: // W lewo
                motorLeft();
                break;
              case 0x00: // Stop
              default:
                motorStop();
                break;
            }
        }
    }
};

// 6. GŁÓWNA FUNKCJA SETUP
void setup() {
  // Konfiguracja pinów silnika
  pinMode(AIN1, OUTPUT);
  pinMode(AIN2, OUTPUT);

  // Bezpieczne zatrzymanie silnika na starcie
  motorStop(); 

  // Tworzenie unikalnej nazwy urządzenia
  char deviceName[20];
  uint8_t mac[6];
  esp_read_mac(mac, ESP_MAC_WIFI_STA);
  sprintf(deviceName, "Motor-%02X:%02X:%02X", mac[3], mac[4], mac[5]);

  // Inicjalizacja BLE
  BLEDevice::init(deviceName);
  BLEServer *pServer = BLEDevice::createServer();
  
  BLEService *pService = pServer->createService(SERVICE_UUID);

  BLECharacteristic *pMotorCharacteristic = pService->createCharacteristic(
                                         MOTOR_CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_WRITE
                                       );
  pMotorCharacteristic->setCallbacks(new MotorCharacteristicCallbacks());

  pService->start();

  // Rozpocznij advertising (rozgłaszanie)
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  BLEDevice::startAdvertising();
}

// 7. GŁÓWNA PĘTLA PROGRAMU
void loop() {
  // Pętla może pozostać pusta, cała logika jest w callbackach BLE
  delay(2000);
}