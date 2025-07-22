---
difficulty: Easy
---

# Команды

Плагин Typewriter предоставляет несколько полезных команд. Ниже приведена таблица с их описанием:

:::tip[Типы]
Некоторые аргументы необязательны `[]`, а некоторые обязательны `<>`.\
Аргумент записи принимает её идентификатор и имя.
:::

| Название команды                                     | Описание                                            | Права                     |
| ------------------------------------------------ | ------------------------------------------------------ | ------------------------------- |
| `/tw connect`                                    | Подключает вас к панели Typewriter.                  | typewriter.connect              |
| `/tw clearChat`                                  | Очищает чат так же, как это делает Typewriter. | typewriter.clearChat            |
| `/tw cinematic <start/stop> <pageName> [player]` | Проигрывает выбранную сцену для указанного игрока.      | typewriter.cinematic.start/stop |
| `/tw reload`                                     | Перезагружает плагин.                                    | typewriter.reload               |
| `/tw facts [player]`                             | Показывает все факты определённого игрока.               | typewriter.facts                |
| `/tw facts set <factEntry> <value> [player]`     | Устанавливает значение факта игрока                       | typewriter.facts.set            |
| `/tw facts reset`                                | Сбрасывает все факты игрока              | typewriter.facts.reset          |
| `/tw trigger <entry> [player]`                   | Запускает запись для указанного игрока.               | typewriter.trigger              |
| `/tw assets clean`                               | Удаляет неиспользуемые ресурсы                   | typewriter.assets.clean         |
| `/tw fire <entry> [player]`                      | Вызывает событие огня для записи.                    | typewriter.fire                 |
| `/tw manifest inspect [player]`                  | Показывает активные манифесты игрока               | typewriter.manifest.inspect     |
| `/tw quest track <questEntry> [player]`          | Начинает отслеживать квест игрока.                  | typewriter.quest.track          |
| `/tw untrack [player]`                           | Прекращает отслеживание квеста игрока.                        | typewriter.quest.untrack        |
| `/tw roadNetwork edit <roadNetworkEntry>`        | Редактирует дорожную сеть.                                    | typewriter.roadNetwork.edit     |

