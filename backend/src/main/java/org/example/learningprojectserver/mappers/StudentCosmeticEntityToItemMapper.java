package org.example.learningprojectserver.mappers;

import org.example.learningprojectserver.dto.QuestDTOs;
import org.example.learningprojectserver.entities.StudentCosmeticEntity;
import org.example.learningprojectserver.service.quest.QuestCatalog;
import org.springframework.stereotype.Component;

/** An unlocked cosmetic, described from the catalog; null when the catalog no longer has the item. */
@Component
public class StudentCosmeticEntityToItemMapper implements Mapper<StudentCosmeticEntity, QuestDTOs.CosmeticItem> {

    @Override
    public QuestDTOs.CosmeticItem apply(StudentCosmeticEntity entity) {
        QuestCatalog.Item item = QuestCatalog.item(entity.getItemCode());
        if (item == null) {
            return null;
        }
        return describe(item, true, entity.isEquipped());
    }

    public static QuestDTOs.CosmeticItem describe(QuestCatalog.Item item, boolean unlocked, boolean equipped) {
        QuestCatalog.Quest quest = QuestCatalog.questOfItem(item.code());
        return new QuestDTOs.CosmeticItem(item.code(), item.kind(), item.name(), item.emoji(), item.tone(),
                item.decor(), quest == null ? null : quest.title(), unlocked, equipped);
    }
}
