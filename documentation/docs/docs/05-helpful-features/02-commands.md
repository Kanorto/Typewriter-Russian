---
difficulty: Easy
---

# Команды

Плагин Typewriter содержит несколько полезных команд. В таблице ниже приведён их список:

:::tip[Types]
Некоторые аргументы необязательны `[]`, а другие обязательны `<>`.\
Аргумент записи принимает идентификатор и имя записи.
:::

| Команда | Описание | Права |
| ------- | -------- | ----- |
| `/tw connect` | Подключиться к панели Typewriter. | typewriter.connect |
| `/tw clearChat` | Очистить чат так же, как это делает Typewriter. | typewriter.clearChat |
| `/tw cinematic <start/stop> <pageName> [player]` | Запустить кинематику. | typewriter.cinematic.start/stop |
| `/tw reload` | Перезагрузить плагин. | typewriter.reload |
| `/tw facts [player]` | Показать все факты указанного игрока. | typewriter.facts |
| `/tw facts set <factEntry> <value> [player]` | Изменить значение факта игрока. | typewriter.facts.set |
| `/tw facts reset` | Сбросить все факты игрока. | typewriter.facts.reset |
| `/tw trigger <entry> [player]` | Запустить запись для указанного игрока. | typewriter.trigger |
| `/tw assets clean` | Удалить неиспользуемые ресурсы. | typewriter.assets.clean |
| `/tw fire <entry> [player]` | Запустить событие типа fire. | typewriter.fire |
| `/tw manifest inspect [player]` | Просмотреть активные манифесты игрока. | typewriter.manifest.inspect |
| `/tw quest track <questEntry> [player]` | Начать отслеживание квеста для игрока. | typewriter.quest.track |
| `/tw untrack [player]` | Прекратить отслеживать квест игрока. | typewriter.quest.untrack |
| `/tw roadNetwork edit <roadNetworkEntry>` | Редактировать roadNetwork. | typewriter.roadNetwork.edit |
