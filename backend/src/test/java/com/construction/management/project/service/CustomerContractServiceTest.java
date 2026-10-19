package com.construction.management.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.construction.management.project.api.CustomerContractResponse;
import com.construction.management.project.domain.ContractFile;
import com.construction.management.project.domain.Project;
import com.construction.management.project.repository.ContractChangeRepository;
import com.construction.management.project.repository.ContractFileRepository;
import com.construction.management.project.repository.CustomerContractItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CustomerContractServiceTest {

	@TempDir
	Path uploadRoot;

	@Test
	void deleteFileRemovesStoredFileAndDatabaseRecord() throws Exception {
		ProjectRepository projectRepository = mock(ProjectRepository.class);
		CustomerContractItemRepository itemRepository = mock(CustomerContractItemRepository.class);
		ContractFileRepository fileRepository = mock(ContractFileRepository.class);
		ContractChangeRepository contractChangeRepository = mock(ContractChangeRepository.class);
		Project project = mock(Project.class);
		Path projectDirectory = Files.createDirectories(uploadRoot.resolve("project-1"));
		Path storedFile = Files.writeString(projectDirectory.resolve("stored-contract.pdf"), "contract");
		ContractFile contractFile = new ContractFile(
				"file-1",
				"project-1",
				"contract.pdf",
				storedFile.toString(),
				"CONTRACT"
		);

		when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));
		when(project.getProjectId()).thenReturn("project-1");
		when(fileRepository.findById("file-1")).thenReturn(Optional.of(contractFile));
		when(fileRepository.findByProjectIdOrderByUploadedAtDesc("project-1")).thenReturn(List.of());
		when(itemRepository.findByProjectIdOrderByWorkCategoryAsc("project-1")).thenReturn(List.of());
		when(contractChangeRepository.findByProjectIdAndApprovalStatus(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
				.thenReturn(List.of());

		CustomerContractService service = new CustomerContractService(
				projectRepository,
				itemRepository,
				fileRepository,
				contractChangeRepository,
				uploadRoot.toString()
		);

		CustomerContractResponse response = service.deleteFile("project-1", "file-1");

		assertThat(storedFile).doesNotExist();
		assertThat(response.files()).isEmpty();
		verify(fileRepository).delete(contractFile);
		verify(fileRepository).flush();
	}
}
