#include <Stepper.h>

const int stepsPerRevolution = 2048;

Stepper myStepper(stepsPerRevolution, 11, 9, 10, 8);

int cds = A0;
boolean val_previous, val_current;

int redPin = 3;
int bluePin = 2;

int speaker = 7;


void setup() {
  // put your setup code here, to run once:
  myStepper.setSpeed(14);
  Serial.begin(9600);
  pinMode(speaker, OUTPUT);
  Serial1.begin(9600);
  val_previous = false;
  val_current = false;

}

void loop() {
  // put your main code here, to run repeatedly:
  char read_data;
  int cds_value = analogRead(cds);

  Serial.print("lightSensor:");
  Serial.println(cds_value);
  delay(500);

  if (cds_value < 300)
  {
    setColor(255, 0, 0); //red
    delay(1000);
    tone(speaker, 523, 2000); 1
    delay(500);
    noTone(speaker);
    Serial.print("state:");
    Serial.println("약 복용을 하지 않았습니다");
    val_current = true;
  }

  if (cds_value > 300)
  {

    setColor(0, 0, 255); //blue
    delay(1000);
    Serial.print("state:");
    Serial.println("약 복용을 잘 하셨습니다.");
    val_current = false;
  }

  if (val_current != val_previous)
  {
    val_previous = val_current;
    Serial.println(val_current);
    sendData(cds_value);
    delay(1000);
  }

  if (Serial1.available())
  {
    char data = (char)Serial1.read();

    if (data == '1')
    {
      every_pill();
      Serial.println("월요일분 약");

    }

    else if (data == '2')
    {
      every_pill();
      Serial.println("화요일분 약");
    }

    else if (data == '3')
    {
      every_pill();
      Serial.println("수요일분 약");
    }

    else if (data == '4')
    {
      every_pill();
      Serial.println("목요일분 약");
    }

    else if (data == '5')
    {
      every_pill();
      Serial.println("금요일분 약");
    }

    else if (data == '6')
    {
      end_pill();
      Serial.println("일주일분 약 복용완료!!");
    }
  }

  if (Serial.available())
  {
    read_data = Serial.read();

    if (read_data == '1')
    {
      Serial.println("월요일분 약");
      every_pill();
    }

    else if (read_data == '2')
    {
      Serial.println("화요일분 약");
      every_pill();
    }

    else if (read_data == '3')
    {
      Serial.println("수요일분 약");
      every_pill();
    }

    else if (read_data == '4')
    {
      Serial.println("목요일분 약");
      every_pill();
    }

    else if (read_data == '5')
    {
      Serial.println("금요일분 약");
      every_pill();
    }

    else if (read_data == '6')
    {
      Serial.println("일주일분 약 복용완료!!");
      end_pill();
    }
  }
}

//요일 별 복용
void every_pill()
{
  for (int i = 0; i < 342; i++)
  {
    myStepper.step(1);
    delay(3);
  }
}

//약 복용 완료
void end_pill()
{
  for (int i = 0; i < 1700; i++)
  {
    myStepper.step(-1);
    delay(3);
  }
}

//조도 센서값 보내기 위한 함수
void sendData(int value)
{
  String message = String(value) + '\n';
  Serial1.print(message);
}


//RGB 색상 설정 함수
void setColor(int red, int green, int blue)
{
  analogWrite(redPin, red);
  analogWrite(greenPin, green);
  analogWrite(bluePin, blue);
}

