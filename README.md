# Sandevistan Origin (Forge 1.20.1 addon for Origins 1.10.0.9)

## Сборка jar (нужны JDK 17 и Gradle 8.1.1)
- Windows: двойной клик по `build.bat`
- Linux/macOS: `./build.sh`
- Без установки ничего: залить папку на GitHub -> вкладка Actions -> "Build mod jar" -> Artifacts.

Готовый файл: `build/libs/sandevistan_origin-1.20.1-1.0.0.jar` (не берите `-sources`).
Положить в `mods` вместе с Origins, Apoli, Calio.

## Игра
Выбрать расу "Сандевистан". Клавиша основной способности Origins (по умолчанию Z) вкл/выкл имплант.
- Время в радиусе 15 блоков замедлено (25% скорости), радужные послеобразы, звуки вкл/выкл.
- Нагрев: полоска в action bar. ~30 с работы -> перегрев: имплант выключается, урон 2 сердца,
  тошнота, 15 с блокировки (эффект "Перегрев"). Остывает, когда выключен.
- Экран: радужная виньетка и расширенный FOV; красная пульсация при перегреве.

Настройки: SandevistanMod.java (RADIUS, SLOW_FACTOR, HEAT_MAX, COOL_RATE, OVERHEAT_COOLDOWN, OVERHEAT_DAMAGE).

## Анимация активации
Кадры лежат в `assets/sandevistan_origin/textures/gui/activation.png` (спрайт-лист 6x6, кадр 384x216, 34 кадра).
Чтобы заменить на другую анимацию — подставьте свой лист с тем же раскладом (или поправьте константы в ScreenEffectHandler).
