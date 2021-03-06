package codes.mydna.sequence_bank.services.impl;

import codes.mydna.auth.common.models.User;
import codes.mydna.rest.exceptions.NotFoundException;
import codes.mydna.rest.utils.EntityList;
import codes.mydna.rest.utils.QueryUtil;
import codes.mydna.rest.validation.Assert;
import codes.mydna.sequence_bank.entities.DnaEntity;
import codes.mydna.sequence_bank.lib.Dna;
import codes.mydna.sequence_bank.lib.Sequence;
import codes.mydna.sequence_bank.lib.enums.SequenceAccessType;
import codes.mydna.sequence_bank.lib.enums.SequenceType;
import codes.mydna.sequence_bank.mappers.DnaMapper;
import codes.mydna.sequence_bank.mappers.SequenceMapper;
import codes.mydna.sequence_bank.services.DnaService;
import codes.mydna.sequence_bank.services.SequenceService;
import codes.mydna.sequence_bank.util.AuthorizationUtil;
import com.kumuluz.ee.rest.beans.QueryParameters;
import com.kumuluz.ee.rest.utils.JPAUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class DnaServiceImpl implements DnaService {

    private static final Logger LOG = Logger.getLogger(DnaServiceImpl.class.getName());
    private String uuid;

    @Inject
    private EntityManager em;

    @Inject
    private SequenceService sequenceService;

    @PostConstruct
    private void pc() {
        uuid = UUID.randomUUID().toString();
        LOG.info(DnaServiceImpl.class.getSimpleName() + " with ID '" + uuid + "' is about to be initialized");
    }

    @PreDestroy
    private void pd() {
        LOG.info(DnaServiceImpl.class.getSimpleName() + " with ID '" + uuid + "' is about to be destroyed");
    }

    @Override
    public EntityList<Dna> getDnas(QueryParameters qp, User user) {

        String userId = (user == null) ? null : user.getId();

        long limit = qp.getLimit();
        long offset = qp.getOffset();
        QueryParameters queryParameters = QueryUtil.removeLimitAndOffset(qp);

        List<Dna> dnas = JPAUtils.queryEntities(em, DnaEntity.class, queryParameters)
                .stream()
                .filter(entity ->
                        entity.getOwnerId().equals(userId) || entity.getAccess().equals(SequenceAccessType.PUBLIC)
                )
                .skip(offset)
                .limit(limit)
                .map(DnaMapper::fromEntityLazy)
                .collect(Collectors.toList());

        Long count = JPAUtils.queryEntitiesCount(em, DnaEntity.class, queryParameters, (p, cb, r)
                -> (cb.and(p, cb.or(cb.equal(r.get("access"), SequenceAccessType.PUBLIC), cb.equal(r.get("ownerId"), userId)))));

        return new EntityList<>(dnas, count);
    }

    @Override
    public Dna getDna(String id, User user) {

        Assert.fieldNotEmpty(id, "id");

        try {
            DnaEntity entity = (DnaEntity) em.createNamedQuery(DnaEntity.GET_PUBLIC_OR_BY_OWNER_ID)
                    .setParameter("id", id)
                    .setParameter("owner_id", user != null ? user.getId() : null)
                    .getSingleResult();
            return DnaMapper.fromEntity(entity);

        } catch (NoResultException e) {
            throw new NotFoundException(Dna.class, id);
        }
    }

    @Override
    public Dna insertDna(Dna dna, User user) {

        Assert.userNotNull(user);
        Assert.objectNotNull(dna, Dna.class);
        Assert.fieldNotEmpty(dna.getName(), "name", Dna.class);

        AuthorizationUtil.verifyModification(dna.getAccess(), user);

        DnaEntity dnaEntity = DnaMapper.toEntity(dna);
        dnaEntity.setOwnerId(user.getId());
        dnaEntity.setAccess(dna.getAccess() == null
                ? SequenceAccessType.PRIVATE
                : dna.getAccess());

        Sequence insertedSequence = sequenceService.insertSequence(dna.getSequence(), SequenceType.DNA, user);
        dnaEntity.setSequence(SequenceMapper.toEntity(insertedSequence));

        em.getTransaction().begin();
        em.persist(dnaEntity);
        em.getTransaction().commit();

        return DnaMapper.fromEntity(dnaEntity);
    }

    @Override
    public Dna updateDna(Dna dna, String id, User user) {

        Assert.userNotNull(user);
        Assert.fieldNotNull(id, "id");
        Assert.objectNotNull(dna, Dna.class);

        DnaEntity old = getDnaEntity(id, user);
        if (old == null) throw new NotFoundException(Dna.class, id);

        DnaEntity entity = DnaMapper.toEntity(dna);
        entity.setId(id);
        entity.setCreated(old.getCreated());
        entity.setOwnerId(old.getOwnerId());
        entity.setAccess(dna.getAccess());

        // Check if entity's access will change
        if(entity.getAccess() != null && entity.getAccess() != old.getAccess()) {

            // If access will change, verify that action is permitted
            AuthorizationUtil.verifyModification(dna.getAccess(), user);
        }

        Sequence updatedSequence = sequenceService.updateSequence(dna.getSequence(), SequenceType.DNA, old.getSequence().getId(), user);
        entity.setSequence(SequenceMapper.toEntity(updatedSequence));

        // dynamic update of base sequence
        if(entity.getName() == null)
            entity.setName(old.getName());
        if(entity.getAccess() == null)
            entity.setAccess(old.getAccess());

        em.getTransaction().begin();
        em.merge(entity);
        em.getTransaction().commit();

        return DnaMapper.fromEntity(getDnaEntity(entity.getId(), user));
    }

    @Override
    public boolean removeDna(String id, User user) {

        Assert.userNotNull(user);

        DnaEntity entity = getDnaEntity(id, user);

        if (entity == null)
            return false;

        em.getTransaction().begin();
        em.remove(entity);
        em.getTransaction().commit();

        return true;
    }

    public DnaEntity getDnaEntity(String id, User user) {
        try {
            return (DnaEntity) em.createNamedQuery(DnaEntity.GET_PUBLIC_OR_BY_OWNER_ID)
                    .setParameter("id", id)
                    .setParameter("owner_id", user.getId())
                    .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }

}
