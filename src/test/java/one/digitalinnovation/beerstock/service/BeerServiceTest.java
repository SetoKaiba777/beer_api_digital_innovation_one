package one.digitalinnovation.beerstock.service;

import one.digitalinnovation.beerstock.builder.BeerDTOBuilder;
import one.digitalinnovation.beerstock.dto.BeerDTO;
import one.digitalinnovation.beerstock.entity.Beer;
import one.digitalinnovation.beerstock.exception.BeerAlreadyRegisteredException;
import one.digitalinnovation.beerstock.exception.BeerNotFoundException;
import one.digitalinnovation.beerstock.exception.BeerStockExceededException;
import one.digitalinnovation.beerstock.exception.NegativeBeerStockException;
import one.digitalinnovation.beerstock.mapper.BeerMapper;
import one.digitalinnovation.beerstock.repository.BeerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BeerServiceTest {

    private static final long INVALID_ID = 2L;

    private static BeerMapper beerMapper = BeerMapper.INSTANCE;

    @Mock
    BeerRepository beerRepository;

    @InjectMocks
    BeerService beerService;

    @Test
    void whenBeerInformedThenItShouldBeCreated() throws BeerAlreadyRegisteredException {
        //Inicializando as entidades
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer beerToSave = beerMapper.toModel(beerDTO);

        //Inicializando comportamento do Mock
        when(beerRepository.save(beerToSave)).thenReturn(beerToSave);
        when(beerRepository.findByName(beerToSave.getName())).thenReturn(Optional.empty());

        //Teste
        var beerOutput = beerService.createBeer(beerDTO);

        assertThat(beerOutput.getId(), is(equalTo(beerDTO.getId())));
        assertThat(beerOutput.getName(), is(equalTo(beerDTO.getName())));
        assertThat(beerOutput, is(equalTo(beerDTO)));

    }

    @Test
    void whenAlreadyRegisteredBeerInformedThenAnExceptionShouldBeThrown() {
        //Inicializando as entidades
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer beerToSave = beerMapper.toModel(beerDTO);

        //Inicializando comportamento do Mock
        when(beerRepository.findByName(beerToSave.getName())).thenReturn(Optional.of(beerToSave));

        //Teste
        assertThrows(BeerAlreadyRegisteredException.class, () -> beerService.createBeer(beerDTO));
    }

    @Test
    void whenValidBeerNameIsGivenThenReturnABeer() throws BeerNotFoundException {
        //Inicializando as entidades
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer beer = beerMapper.toModel(beerDTO);

        //Inicializando comportamento do Mock
        when(beerRepository.findByName(any())).thenReturn(Optional.of(beer));

        //teste
        var beerOutput = beerService.findByName(beerDTO.getName());
        assertThat(beerDTO, is(beerOutput));
    }

    @Test
    void whenInvalidBeerNameIsGivenThenReturnAException() {
        String name = "Invalid beer name";

        //Inicializando comportamento do Mock
        when(beerRepository.findByName(name)).thenReturn(Optional.empty());

        //teste
        assertThrows(BeerNotFoundException.class, () -> beerService.findByName(name));
    }

    @Test
    void whenListBeerIsCalledReturnAListOfAllBeers() {
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer beer = beerMapper.toModel(beerDTO);

        //Inicializando comportamento do Mock
        when(beerRepository.findAll()).thenReturn(Collections.singletonList(beer));

        //teste;
        var listAllDTO = beerService.listAll();
        assertThat(listAllDTO, is(not(empty())));
        assertThat(listAllDTO.get(0), is(equalTo(beerDTO)));
    }

    @Test
    void whenListBeerIsCalledReturnAEmptyList() {

        //Inicializando comportamento do Mock
        when(beerRepository.findAll()).thenReturn(Collections.EMPTY_LIST);

        //teste;
        var listAllDTO = beerService.listAll();
        assertThat(listAllDTO, is(empty()));
    }

    @Test
    void whenExclusionBeerIsCalledWithAValidIdThenABeerShouldBeExcluded() throws BeerNotFoundException {
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer beer = beerMapper.toModel(beerDTO);

        when(beerRepository.findById(beer.getId())).thenReturn(Optional.of(beer));
        doNothing().when(beerRepository).deleteById(beer.getId());

        beerService.deleteById(beer.getId());

        verify(beerRepository, times(1)).findById(beer.getId());
        verify(beerRepository, times(1)).deleteById(beer.getId());
    }

    @Test
    void whenExclusionBeerIsCalledWithAInalidIdThenReturnException() {

        Long invalidId = 1L;

        when(beerRepository.findById(invalidId)).thenReturn(Optional.empty());

        assertThrows(BeerNotFoundException.class,
                () -> beerService.deleteById(invalidId));
    }

    @Test
    void whenIncrementIsCalledThenIncrementBeerInStock() throws BeerNotFoundException, BeerStockExceededException {
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBear = beerMapper.toModel(beerDTO);

        when(beerRepository.findById(expectedBear.getId()))
                .thenReturn(Optional.of(expectedBear));
        when(beerRepository.save(expectedBear))
                .thenReturn(expectedBear);

        int quantityToIncrement = 10;
        int expectedQuantityAfterIncrement = beerDTO.getQuantity() + quantityToIncrement;

        BeerDTO incrementedBeerDTO = beerService.increment(expectedBear.getId(),
                quantityToIncrement);
        assertThat(expectedQuantityAfterIncrement, equalTo(incrementedBeerDTO.getQuantity()));
        assertThat(expectedQuantityAfterIncrement, lessThan(expectedBear.getMax()));
    }

    @Test
    void whenIncrementAfterSumIsGreaterThenReturnStockException() throws BeerNotFoundException, BeerStockExceededException {
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBear = beerMapper.toModel(beerDTO);

        when(beerRepository.findById(expectedBear.getId()))
                .thenReturn(Optional.of(expectedBear));

        int quantityToIncrement = 1000;

        assertThrows(BeerStockExceededException.class, () -> beerService.increment(expectedBear.getId(),
                quantityToIncrement));
    }

    @Test
    void whenIncrementIsCalledWithInvalidIdThenReturnException() throws Exception {
        int quantityToIncrement = 10;

        when(beerRepository.findById(INVALID_ID))
                .thenReturn(Optional.empty());

        assertThrows(BeerNotFoundException.class, () -> beerService.increment(INVALID_ID,
                quantityToIncrement));
    }

    @Test
    void whenDecrementIsCalledThenDecrementBeerInStock() throws BeerNotFoundException, BeerStockExceededException, NegativeBeerStockException {
        BeerDTO beerDTO = BeerDTOBuilder.builder().quantity(12)
                .build()
                .toBeerDTO();
        Beer expectedBear = beerMapper.toModel(beerDTO);


        when(beerRepository.findById(expectedBear.getId()))
                .thenReturn(Optional.of(expectedBear));
        when(beerRepository.save(expectedBear))
                .thenReturn(expectedBear);


        int quantityToDecrement = 10;
        int expectedQuantityAfterDecrement = beerDTO.getQuantity() - quantityToDecrement;

        BeerDTO decrementedBeerDTO = beerService.decrement(expectedBear.getId(),
                quantityToDecrement);
        assertThat(expectedQuantityAfterDecrement, equalTo(decrementedBeerDTO.getQuantity()));
    }

    @Test
    void whenDecrementIsGreaterThanQuantityThenReturnNegativeBeerException()  {
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBear = beerMapper.toModel(beerDTO);

        when(beerRepository.findById(expectedBear.getId()))
                .thenReturn(Optional.of(expectedBear));

        int quantityToIncrement = 1000;

        assertThrows(NegativeBeerStockException.class, () -> beerService.decrement(expectedBear.getId(),
                quantityToIncrement));
    }



    @Test
    void whenDecrementIsCalledWithInvalidIdThenReturnException() throws Exception {
        int quantityToDecrement = 10;

        when(beerRepository.findById(INVALID_ID))
                .thenReturn(Optional.empty());

        assertThrows(BeerNotFoundException.class, () -> beerService.decrement(INVALID_ID,
                quantityToDecrement));
    }
}
