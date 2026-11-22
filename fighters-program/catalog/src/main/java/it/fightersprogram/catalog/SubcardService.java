package it.fightersprogram.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.domain.Subcard;
import it.fightersprogram.catalog.domain.SubcardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubcardService {

    private final SubcardRepository subcardRepository;

    public List<SubcardView> findAll() {
        return subcardRepository.findAllByOrderByCodeAsc().stream().map(SubcardService::toView).toList();
    }

    public Map<String, SubcardView> findByCodes(Collection<String> codes) {
        return subcardRepository.findAllByCodeIn(codes).stream()
                .map(SubcardService::toView)
                .collect(Collectors.toMap(SubcardView::code, Function.identity()));
    }

    private static SubcardView toView(Subcard subcard) {
        return new SubcardView(subcard.getCode(), subcard.getName(), subcard.getDescription(),
                subcard.getAtkPercent(), subcard.getDefPercent(), subcard.getVelPercent(), subcard.getCrtPercent());
    }
}
