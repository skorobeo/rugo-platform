package ru.rugo.app.web.dto;

/** Количество карточек по категории/типу — для сортировки списков в мини-приложении. */
public record CategoryCountDto(String key, long count) {
}
