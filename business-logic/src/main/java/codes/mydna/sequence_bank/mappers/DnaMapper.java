package codes.mydna.sequence_bank.mappers;

import codes.mydna.sequence_bank.entities.DnaEntity;
import codes.mydna.sequence_bank.lib.Dna;

public class DnaMapper {

    public static Dna fromEntityLazy(DnaEntity entity){
        if(entity == null)
            return null;
        Dna dna = new Dna();
        BaseSequenceMapper.fromEntityLazy(entity, dna);
        return dna;
    }

    public static Dna fromEntity(DnaEntity entity){
        if(entity == null)
            return null;
        Dna dna = new Dna();
        BaseSequenceMapper.fromEntity(entity, dna);
        return dna;
    }

    public static DnaEntity toEntity(Dna dna){
        if(dna == null)
            return null;
        DnaEntity entity = new DnaEntity();
        BaseSequenceMapper.toEntity(dna, entity);
        return entity;
    }

}
