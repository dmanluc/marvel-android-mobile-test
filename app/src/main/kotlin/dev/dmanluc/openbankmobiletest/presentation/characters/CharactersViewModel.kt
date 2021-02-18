package dev.dmanluc.openbankmobiletest.presentation.characters

import androidx.lifecycle.*
import dev.dmanluc.openbankmobiletest.domain.model.Character
import dev.dmanluc.openbankmobiletest.domain.usecase.GetCharactersUseCase
import dev.dmanluc.openbankmobiletest.utils.AppDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CharactersViewModel(
    private val getCharactersUseCase: GetCharactersUseCase,
    private val appDispatchers: AppDispatchers
) : ViewModel() {

    private val mutableCharacterListLiveData = MutableLiveData<List<Character>>()
    val characterListLiveData: LiveData<List<Character>> get() = mutableCharacterListLiveData

    init {
        viewModelScope.launch {
            getCharactersUseCase().collect { value ->
                value.fold(ifLeft = {}) { characterList ->
                    mutableCharacterListLiveData.value = characterList
                }
            }
        }
    }

}